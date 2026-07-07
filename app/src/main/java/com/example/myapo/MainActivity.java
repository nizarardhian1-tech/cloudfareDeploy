package com.example.myapo;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.content.ContextCompat;

import com.example.myapo.network.CloudflareApi;
import com.example.myapo.utils.UiHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    // ── Views ────────────────────────────────────────────────────────────────
    private LinearLayout linearWorkersList;
    private TextView tvAccount, tvSubdomain;
    private TextView tvStatStatus, tvStatLimit, tvStatCount;
    private TextView tvListPlaceholder;
    private View viewStatusDot;
    private MaterialButton tvSwitchAccount;
    private MaterialButton tvRefreshBtn;
    private MaterialButton tvCreateWorkerBtn;
    private MaterialButton btnManageLicenses;
    private TextInputEditText etSearchWorker;

    // ── State ─────────────────────────────────────────────────────────────────
    private SharedPreferences prefs;
    private JSONArray cachedWorkersArray = new JSONArray();
    private boolean isFetching = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Handle system bar insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.coordinator_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return insets;
        });

        prefs = getSharedPreferences("mode", MODE_PRIVATE);

        bindViews();
        setupListeners();
        checkTokenStatus();
        loadCachedWorkers();
        fetchWorkersList(false);
        fetchAccountSubdomain();
    }

    // ── View Binding ──────────────────────────────────────────────────────────
    private void bindViews() {
        linearWorkersList = findViewById(R.id.linear_workers_list);
        tvAccount         = findViewById(R.id.tv_account);
        tvSubdomain       = findViewById(R.id.tv_subdomain);
        tvStatStatus      = findViewById(R.id.tv_stat_status);
        tvStatLimit       = findViewById(R.id.tv_stat_limit);
        tvStatCount       = findViewById(R.id.tv_stat_count);
        tvListPlaceholder = findViewById(R.id.tv_list_placeholder);
        viewStatusDot     = findViewById(R.id.view_status_dot);
        tvSwitchAccount   = findViewById(R.id.tv_switch_account);
        tvRefreshBtn      = findViewById(R.id.tv_refresh_btn);
        tvCreateWorkerBtn = findViewById(R.id.tv_create_worker_btn);
        btnManageLicenses = findViewById(R.id.btn_manage_licenses);
        etSearchWorker    = findViewById(R.id.et_search_worker);
    }

    private void setupListeners() {
        tvAccount.setOnClickListener(v -> showApiTokenDialog());
        tvSwitchAccount.setOnClickListener(v -> showSwitchAccountDialog());
        tvRefreshBtn.setOnClickListener(v -> {
            fetchWorkersList(true);
            fetchAccountSubdomain();
        });
        tvCreateWorkerBtn.setOnClickListener(v -> showCreateWorkerDialog());
        btnManageLicenses.setOnClickListener(v -> startActivity(new Intent(this, LicenseActivity.class)));

        etSearchWorker.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) {
                filterWorkers(s.toString());
            }
            public void afterTextChanged(Editable s) {}
        });
    }

    // ── Account State ─────────────────────────────────────────────────────────
    private void checkTokenStatus() {
        String token = prefs.getString("cf_api_token", "");
        String name  = prefs.getString("cf_account_name", "");
        if (token.isEmpty()) {
            tvAccount.setText("Tap to set up API Token");
            tvSubdomain.setVisibility(View.GONE);
            tvSwitchAccount.setVisibility(View.GONE);
        } else {
            tvAccount.setText(name.isEmpty() ? "Account configured" : name);
            tvSwitchAccount.setVisibility(View.VISIBLE);
        }
    }

    // ── Switch / Delete Account ───────────────────────────────────────────────
    private void showSwitchAccountDialog() {
        String json = prefs.getString("cf_accounts_list", "[]");
        final String[] names;
        final String[] data;
        try {
            JSONArray arr = new JSONArray(json);
            names = new String[arr.length()];
            data  = new String[arr.length()];
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                String id = o.getString("account_id");
                names[i] = o.getString("name") + " (" + id.substring(0, Math.min(8, id.length())) + "...)";
                data[i]  = o.toString();
            }
        } catch (Exception e) {
            UiHelper.showError(this, "Failed to load accounts.");
            return;
        }
        if (names.length == 0) {
            UiHelper.showMessage(this, "No saved accounts. Set up a token first.");
            return;
        }
        new MaterialAlertDialogBuilder(this)
            .setTitle("Switch Account")
            .setItems(names, (d, which) -> {
                try {
                    JSONObject sel = new JSONObject(data[which]);
                    prefs.edit()
                        .putString("cf_api_token",    sel.getString("token"))
                        .putString("cf_account_id",   sel.getString("account_id"))
                        .putString("cf_account_name", sel.getString("name"))
                        .apply();
                    UiHelper.showMessage(this, "Switched to: " + sel.getString("name"));
                    checkTokenStatus();
                    fetchWorkersList(true);
                    fetchAccountSubdomain();
                } catch (Exception ex) {
                    UiHelper.showError(this, "Failed to switch account.");
                }
            })
            .setNegativeButton("Add Account", (d, w) -> showApiTokenDialog())
            .setNeutralButton("Remove Account", (d, w) -> showDeleteAccountDialog())
            .show();
    }

    private void showDeleteAccountDialog() {
        String json = prefs.getString("cf_accounts_list", "[]");
        final String[] names;
        final JSONArray arr;
        try {
            arr = new JSONArray(json);
            names = new String[arr.length()];
            for (int i = 0; i < arr.length(); i++)
                names[i] = arr.getJSONObject(i).getString("name");
        } catch (Exception e) {
            UiHelper.showError(this, "Error reading accounts.");
            return;
        }
        if (names.length == 0) return;
        new MaterialAlertDialogBuilder(this)
            .setTitle("Remove Account")
            .setItems(names, (d, which) -> {
                try {
                    arr.remove(which);
                    prefs.edit().putString("cf_accounts_list", arr.toString()).apply();
                    UiHelper.showMessage(this, "Account removed.");
                    checkTokenStatus();
                } catch (Exception e) {
                    UiHelper.showError(this, "Failed to remove.");
                }
            })
            .show();
    }

    // ── API Token Dialog ──────────────────────────────────────────────────────
    private void showApiTokenDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_token_input, null, false);
        EditText etToken = dialogView.findViewById(R.id.et_dialog_token);
        if (etToken == null) {
            // fallback: plain EditText
            etToken = new EditText(this);
            etToken.setHint("Paste Cloudflare API Token…");
            etToken.setSingleLine(true);
        }
        final EditText finalEt = etToken;
        new MaterialAlertDialogBuilder(this)
            .setTitle("Add Cloudflare Account")
            .setMessage("Enter your API Token. Account ID is detected automatically.")
            .setView(dialogView != null ? dialogView : etToken)
            .setPositiveButton("Verify & Save", (d, w) -> {
                String t = finalEt.getText().toString().trim();
                if (t.isEmpty()) { UiHelper.showMessage(this, "Token cannot be empty."); return; }
                prefs.edit().putString("cf_api_token", t).apply();
                checkTokenStatus();
                verifyTokenAndFetchAccountId(true);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    // ── Token Verification ────────────────────────────────────────────────────
    private void verifyTokenAndFetchAccountId(boolean saveToList) {
        String token = prefs.getString("cf_api_token", "");
        if (token.isEmpty()) { UiHelper.showMessage(this, "Token is empty."); return; }
        showLoading(tvListPlaceholder, "Verifying token…");
        new Thread(() -> {
            try {
                CloudflareApi.Response r = CloudflareApi.get("/accounts", token);
                runOnUiThread(() -> {
                    try {
                        JSONObject obj = new JSONObject(r.body);
                        if (obj.optBoolean("success", false)) {
                            JSONArray res = obj.getJSONArray("result");
                            if (res.length() > 0) {
                                JSONObject acct = res.getJSONObject(0);
                                String id   = acct.getString("id");
                                String name = acct.getString("name");
                                prefs.edit()
                                    .putString("cf_account_id",   id)
                                    .putString("cf_account_name", name)
                                    .apply();
                                if (saveToList) saveAccountToPrefs(name, id, token);
                                UiHelper.showMessage(this, "Connected: " + name);
                                fetchWorkersList(false);
                                fetchAccountSubdomain();
                                checkTokenStatus();
                            }
                        } else {
                            handleScopeError(r.body);
                        }
                    } catch (Exception ex) {
                        UiHelper.showError(this, "Failed to parse API response.");
                    }
                });
            } catch (Exception ex) {
                runOnUiThread(() -> UiHelper.showError(this, "Network error: " + ex.getMessage()));
            }
        }).start();
    }

    private void saveAccountToPrefs(String name, String id, String token) {
        try {
            JSONArray arr = new JSONArray(prefs.getString("cf_accounts_list", "[]"));
            for (int i = 0; i < arr.length(); i++) {
                if (arr.getJSONObject(i).getString("account_id").equals(id)) return;
            }
            JSONObject o = new JSONObject();
            o.put("name", name); o.put("account_id", id); o.put("token", token);
            arr.put(o);
            prefs.edit().putString("cf_accounts_list", arr.toString()).apply();
        } catch (Exception ignored) {}
    }

    // ── Scope Error ───────────────────────────────────────────────────────────
    private void handleScopeError(String body) {
        try {
            JSONObject obj = new JSONObject(body);
            JSONArray errors = obj.getJSONArray("errors");
            if (errors.length() > 0) {
                String msg = errors.getJSONObject(0).getString("message");
                String lower = msg.toLowerCase();
                if (lower.contains("permission") || lower.contains("scope") || lower.contains("authorization")) {
                    showScopeDialog(msg); return;
                }
            }
        } catch (Exception ignored) {}
        UiHelper.showError(this, "API error. Check token permissions.");
    }

    private void showScopeDialog(String errorMsg) {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Insufficient Token Permissions")
            .setMessage("Your token lacks required scopes.\n\n" +
                "Error: " + errorMsg + "\n\n" +
                "Required scopes:\n" +
                "• Workers Scripts: Edit\n" +
                "• Workers KV: Edit\n" +
                "• Workers Secrets: Edit")
            .setPositiveButton("Open Token Dashboard", (d, w) ->
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://dash.cloudflare.com/profile/api-tokens"))))
            .setNegativeButton("Close", null)
            .show();
    }

    // ── Create Worker ─────────────────────────────────────────────────────────
    private void showCreateWorkerDialog() {
        final EditText input = new EditText(this);
        input.setHint("script-name (e.g. my-api)");
        input.setSingleLine(true);
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setPadding(dpToPx(24), dpToPx(8), dpToPx(24), 0);
        wrapper.addView(input);
        new MaterialAlertDialogBuilder(this)
            .setTitle("Create New Worker")
            .setMessage("Initialize a new Cloudflare Workers script.")
            .setView(wrapper)
            .setPositiveButton("Create", (d, w) -> {
                String name = input.getText().toString().trim().toLowerCase().replace(" ", "-");
                if (!name.isEmpty()) createWorker(name);
                else UiHelper.showMessage(this, "Name cannot be empty.");
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void createWorker(String scriptName) {
        String token = prefs.getString("cf_api_token", "");
        String acctId = prefs.getString("cf_account_id", "");
        if (token.isEmpty() || acctId.isEmpty()) {
            UiHelper.showMessage(this, "Configure API token first."); return;
        }
        showLoading(tvListPlaceholder, "Creating " + scriptName + "…");
        String starterJs = "addEventListener('fetch', event => {\n" +
                           "  event.respondWith(handleRequest(event.request))\n})\n\n" +
                           "async function handleRequest(request) {\n" +
                           "  return new Response('Hello from " + scriptName + "!', { status: 200 })\n}";
        new Thread(() -> {
            try {
                CloudflareApi.Response r = CloudflareApi.putScript(
                    "/accounts/" + acctId + "/workers/scripts/" + scriptName, token, starterJs);
                runOnUiThread(() -> {
                    if (r.isSuccess()) {
                        UiHelper.showMessage(this, "Worker '" + scriptName + "' created!");
                        fetchWorkersList(false);
                    } else {
                        if (r.body.contains("permission") || r.body.contains("scope"))
                            handleScopeError(r.body);
                        else
                            UiHelper.showError(this, "Create failed: " + r.code);
                    }
                });
            } catch (Exception ex) {
                runOnUiThread(() -> UiHelper.showError(this, "Network error: " + ex.getMessage()));
            }
        }).start();
    }

    // ── Delete Worker ─────────────────────────────────────────────────────────
    private void confirmDeleteWorker(String name) {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Delete Worker")
            .setMessage("Permanently delete '" + name + "'? This cannot be undone.")
            .setPositiveButton("Delete", (d, w) -> deleteWorker(name))
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void deleteWorker(String name) {
        String token  = prefs.getString("cf_api_token",  "");
        String acctId = prefs.getString("cf_account_id", "");
        if (token.isEmpty() || acctId.isEmpty()) return;
        new Thread(() -> {
            try {
                CloudflareApi.Response r = CloudflareApi.delete(
                    "/accounts/" + acctId + "/workers/scripts/" + name, token);
                runOnUiThread(() -> {
                    if (r.isSuccess()) {
                        UiHelper.showMessage(this, "'" + name + "' deleted.");
                        fetchWorkersList(false);
                    } else {
                        UiHelper.showError(this, "Delete failed: " + r.code);
                    }
                });
            } catch (Exception ex) {
                runOnUiThread(() -> UiHelper.showError(this, "Network error: " + ex.getMessage()));
            }
        }).start();
    }

    // ── Workers List ──────────────────────────────────────────────────────────
    private void fetchWorkersList(boolean showProgress) {
        String token  = prefs.getString("cf_api_token",  "");
        String acctId = prefs.getString("cf_account_id", "");
        if (token.isEmpty() || acctId.isEmpty()) {
            tvListPlaceholder.setText("Set your Cloudflare API Token to load scripts.");
            setStatus("Unconfigured", false, 0xFFD96B16);
            return;
        }
        if (isFetching) return;
        isFetching = true;
        if (!showProgress) showLoading(tvListPlaceholder, "Syncing…");
        new Thread(() -> {
            try {
                CloudflareApi.Response r = CloudflareApi.get(
                    "/accounts/" + acctId + "/workers/scripts", token);
                runOnUiThread(() -> {
                    isFetching = false;
                    try {
                        if (!r.isSuccess()) {
                            setStatus("Error", false, 0xFFD6483F);
                            if (r.body.contains("permission") || r.body.contains("scope"))
                                handleScopeError(r.body);
                            return;
                        }
                        JSONObject obj = new JSONObject(r.body);
                        if (obj.optBoolean("success", false)) {
                            JSONArray result = obj.getJSONArray("result");
                            cachedWorkersArray = result;
                            prefs.edit().putString("cf_cached_scripts", r.body).apply();
                            populateWorkersList(result);
                            setStatus("Online", true, 0xFF1E9E6B);
                        } else {
                            setStatus("Rejected", false, 0xFFD6483F);
                            handleScopeError(r.body);
                        }
                    } catch (Exception ex) {
                        setStatus("Parse Error", false, 0xFFD6483F);
                    }
                });
            } catch (Exception ex) {
                runOnUiThread(() -> {
                    isFetching = false;
                    setStatus("Offline", false, 0xFFD6483F);
                    tvListPlaceholder.setText("Network unavailable.");
                });
            }
        }).start();
    }

    private void fetchAccountSubdomain() {
        String token  = prefs.getString("cf_api_token",  "");
        String acctId = prefs.getString("cf_account_id", "");
        if (token.isEmpty() || acctId.isEmpty()) return;
        new Thread(() -> {
            try {
                CloudflareApi.Response r = CloudflareApi.get(
                    "/accounts/" + acctId + "/workers/subdomain", token);
                if (r.isSuccess()) {
                    JSONObject obj = new JSONObject(r.body);
                    if (obj.optBoolean("success", false)) {
                        JSONObject res = obj.optJSONObject("result");
                        if (res != null) {
                            String sub = res.optString("subdomain", "");
                            if (!sub.isEmpty()) {
                                prefs.edit().putString("cf_subdomain", sub).apply();
                                runOnUiThread(() -> {
                                    tvSubdomain.setText(sub + ".workers.dev");
                                    tvSubdomain.setVisibility(View.VISIBLE);
                                });
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
        }).start();
    }

    // ── List Population ───────────────────────────────────────────────────────
    private void populateWorkersList(JSONArray arr) {
        linearWorkersList.removeAllViews();
        if (arr == null || arr.length() == 0) {
            tvStatCount.setText("0");
            tvListPlaceholder.setVisibility(View.VISIBLE);
            tvListPlaceholder.setText("No scripts found. Tap '+ New' to create one.");
            return;
        }
        tvStatCount.setText(String.valueOf(arr.length()));
        tvListPlaceholder.setVisibility(View.GONE);

        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < arr.length(); i++) {
            try {
                JSONObject script = arr.getJSONObject(i);
                final String workerName = script.getString("id");
                String date = script.optString("modified_on", "");
                if (date.contains("T")) date = date.split("T")[0];
                final String displayDate = date;

                View row = inflater.inflate(R.layout.item_worker, linearWorkersList, false);
                ((TextView) row.findViewById(R.id.tv_item_name)).setText(workerName);
                ((TextView) row.findViewById(R.id.tv_item_date)).setText(
                    displayDate.isEmpty() ? "Updated: —" : "Updated: " + displayDate);

                row.setOnClickListener(v -> {
                    Intent intent = new Intent(this, SecundActivity.class);
                    intent.putExtra("WORKER_NAME", workerName);
                    startActivity(intent);
                    overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
                });

                row.findViewById(R.id.btn_item_delete).setOnClickListener(v ->
                    confirmDeleteWorker(workerName));

                UiHelper.animateFadeIn(row);
                linearWorkersList.addView(row);

                // Divider
                if (i < arr.length() - 1) {
                    View divider = new View(this);
                    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 1);
                    p.setMargins(0, 0, 0, 0);
                    divider.setLayoutParams(p);
                    divider.setBackgroundColor(ContextCompat.getColor(this, R.color.md_outline_variant));
                    linearWorkersList.addView(divider);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // ── Search ────────────────────────────────────────────────────────────────
    private void filterWorkers(String query) {
        if (cachedWorkersArray == null || cachedWorkersArray.length() == 0) {
            tvListPlaceholder.setVisibility(View.VISIBLE);
            return;
        }
        if (query.isEmpty()) { populateWorkersList(cachedWorkersArray); return; }
        JSONArray filtered = new JSONArray();
        try {
            for (int i = 0; i < cachedWorkersArray.length(); i++) {
                JSONObject item = cachedWorkersArray.getJSONObject(i);
                if (item.getString("id").toLowerCase().contains(query.toLowerCase()))
                    filtered.put(item);
            }
            populateWorkersList(filtered);
            if (filtered.length() == 0) {
                tvListPlaceholder.setVisibility(View.VISIBLE);
                tvListPlaceholder.setText("No scripts match '" + query + "'.");
            }
        } catch (Exception ignored) {}
    }

    // ── Cache ─────────────────────────────────────────────────────────────────
    private void loadCachedWorkers() {
        String cached = prefs.getString("cf_cached_scripts", "{}");
        if (cached.isEmpty() || cached.equals("{}")) return;
        try {
            JSONObject obj = new JSONObject(cached);
            if (obj.has("result")) {
                cachedWorkersArray = obj.getJSONArray("result");
                populateWorkersList(cachedWorkersArray);
                setStatus("Cached", false, 0xFFD96B16);
            }
        } catch (Exception ignored) {}
    }

    // ── UI Helpers ────────────────────────────────────────────────────────────
    private void setStatus(String label, boolean online, int color) {
        tvStatStatus.setText(label);
        tvStatStatus.setTextColor(color);
        if (viewStatusDot != null) {
            viewStatusDot.setBackgroundResource(
                online ? R.drawable.dot_status_online : R.drawable.dot_status_offline);
        }
    }

    private void showLoading(TextView placeholder, String msg) {
        placeholder.setText(msg);
        placeholder.setVisibility(View.VISIBLE);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
