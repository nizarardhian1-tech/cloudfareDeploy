package com.example.myapo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapo.network.CloudflareApi;
import com.example.myapo.utils.UiHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

public class LicenseActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private LinearLayout linearLicenseBg;
    private LinearLayout cardKvSetup;
    private LinearLayout cardSimulator;
    private LinearLayout layoutTabKeys;
    private LinearLayout layoutTabLogs;
    private LinearLayout containerKeysList;
    private LinearLayout containerLogsList;

    private TextView tvKvNamespaceLabel;
    private TextView tvKvNamespaceId;
    private TextView tvKeysPlaceholder;
    private TextView tvLogsPlaceholder;
    private TextView tvSimulatorTitle;
    private TextView tvSimulatorDesc;
    private TextView tvSimResult;

    private EditText etSimUrl;
    private EditText etSimKey;
    private EditText etSimDevice;

    private MaterialButton btnTabKeys;
    private MaterialButton btnTabLogs;
    private MaterialButton btnGenerateKey;
    private MaterialButton btnCreateNamespace;
    private MaterialButton btnRunSim;

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_license);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.linear_license_bg), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, 0, bars.right, bars.bottom);
            return insets;
        });

        prefs = getSharedPreferences("mode", MODE_PRIVATE);

        bindViews();
        setupToolbar();
        setupListeners();
        updateNamespaceLabel();

        String kvId = prefs.getString("cf_kv_namespace_id", "");
        if (!kvId.isEmpty()) syncKeysAndLogs();

        String subdomain = prefs.getString("cf_subdomain", "");
        if (!subdomain.isEmpty() && etSimUrl != null)
            etSimUrl.setHint("https://your-script." + subdomain + ".workers.dev");
    }

    private void bindViews() {
        toolbar             = findViewById(R.id.toolbar_license);
        linearLicenseBg     = findViewById(R.id.linear_license_bg);
        cardKvSetup         = findViewById(R.id.card_kv_setup);
        cardSimulator       = findViewById(R.id.card_simulator);
        layoutTabKeys       = findViewById(R.id.layout_tab_keys);
        layoutTabLogs       = findViewById(R.id.layout_tab_logs);
        containerKeysList   = findViewById(R.id.container_keys_list);
        containerLogsList   = findViewById(R.id.container_logs_list);
        tvKvNamespaceLabel  = findViewById(R.id.tv_kv_namespace_label);
        tvKvNamespaceId     = findViewById(R.id.tv_kv_namespace_id);
        tvKeysPlaceholder   = findViewById(R.id.tv_keys_placeholder);
        tvLogsPlaceholder   = findViewById(R.id.tv_logs_placeholder);
        tvSimulatorTitle    = findViewById(R.id.tv_simulator_title);
        tvSimulatorDesc     = findViewById(R.id.tv_simulator_desc);
        tvSimResult         = findViewById(R.id.tv_sim_result);
        etSimUrl            = findViewById(R.id.et_sim_url);
        etSimKey            = findViewById(R.id.et_sim_key);
        etSimDevice         = findViewById(R.id.et_sim_device);
        btnTabKeys          = findViewById(R.id.btn_tab_keys);
        btnTabLogs          = findViewById(R.id.btn_tab_logs);
        btnGenerateKey      = findViewById(R.id.btn_generate_key);
        btnCreateNamespace  = findViewById(R.id.btn_create_namespace);
        btnRunSim           = findViewById(R.id.btn_run_sim);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("KV Database & Licenses");
        }
    }

    private void setupListeners() {
        if (tvKvNamespaceId != null) tvKvNamespaceId.setOnClickListener(v -> showNamespaceSetupDialog());
        if (btnTabKeys != null)     btnTabKeys.setOnClickListener(v -> switchTab(true));
        if (btnTabLogs != null)     btnTabLogs.setOnClickListener(v -> switchTab(false));
        if (btnGenerateKey != null) btnGenerateKey.setOnClickListener(v -> showGenerateKeyDialog());
        if (btnCreateNamespace != null) btnCreateNamespace.setOnClickListener(v -> showCreateNamespaceDialog());
        if (btnRunSim != null)      btnRunSim.setOnClickListener(v -> runAPISimulation());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }

    // ── Namespace ─────────────────────────────────────────────────────────────
    private void updateNamespaceLabel() {
        if (tvKvNamespaceId == null) return;
        String id = prefs.getString("cf_kv_namespace_id", "");
        tvKvNamespaceId.setText(id.isEmpty() ? "Tap to set KV Namespace ID…" : id);
    }

    private void showNamespaceSetupDialog() {
        final EditText input = new EditText(this);
        input.setHint("Paste KV Namespace ID…");
        input.setSingleLine(true);
        String cur = prefs.getString("cf_kv_namespace_id", "");
        if (!cur.isEmpty()) input.setText(cur);
        LinearLayout wrap = new LinearLayout(this);
        wrap.setPadding(dpToPx(24), dpToPx(8), dpToPx(24), 0);
        wrap.addView(input);
        new MaterialAlertDialogBuilder(this)
            .setTitle("KV Namespace Setup")
            .setMessage("Enter the KV Namespace ID for key/log storage.")
            .setView(wrap)
            .setPositiveButton("Save", (d, w) -> {
                String id = input.getText().toString().trim();
                if (id.isEmpty()) { UiHelper.showMessage(this, "ID cannot be empty."); return; }
                prefs.edit().putString("cf_kv_namespace_id", id).apply();
                updateNamespaceLabel();
                syncKeysAndLogs();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showCreateNamespaceDialog() {
        final EditText input = new EditText(this);
        input.setHint("Namespace title (e.g. app_licenses)");
        input.setSingleLine(true);
        LinearLayout wrap = new LinearLayout(this);
        wrap.setPadding(dpToPx(24), dpToPx(8), dpToPx(24), 0);
        wrap.addView(input);
        new MaterialAlertDialogBuilder(this)
            .setTitle("Create KV Namespace")
            .setMessage("Creates a new KV namespace in your Cloudflare account via API.")
            .setView(wrap)
            .setPositiveButton("Create", (d, w) -> {
                String name = input.getText().toString().trim().replace(" ", "_");
                if (!name.isEmpty()) createKVNamespace(name);
                else UiHelper.showMessage(this, "Name cannot be empty.");
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void createKVNamespace(String name) {
        String token  = prefs.getString("cf_api_token", "");
        String acctId = prefs.getString("cf_account_id", "");
        if (token.isEmpty() || acctId.isEmpty()) {
            UiHelper.showMessage(this, "API config missing."); return;
        }
        UiHelper.showMessage(this, "Creating namespace…");
        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("title", name);
                CloudflareApi.Response r = CloudflareApi.postJson(
                    "/accounts/" + acctId + "/storage/kv/namespaces", token, body.toString());
                runOnUiThread(() -> {
                    try {
                        if (r.isSuccess()) {
                            JSONObject res = new JSONObject(r.body).getJSONObject("result");
                            String id = res.getString("id");
                            prefs.edit().putString("cf_kv_namespace_id", id).apply();
                            updateNamespaceLabel();
                            UiHelper.showMessage(this, "Namespace created!");
                            syncKeysAndLogs();
                        } else {
                            UiHelper.showError(this, "Failed (" + r.code + ").");
                        }
                    } catch (Exception e) {
                        UiHelper.showError(this, "Parse error.");
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> UiHelper.showError(this, "Error: " + e.getMessage()));
            }
        }).start();
    }

    // ── Tabs ──────────────────────────────────────────────────────────────────
    private void switchTab(boolean showKeys) {
        if (layoutTabKeys == null || layoutTabLogs == null) return;
        layoutTabKeys.setVisibility(showKeys ? View.VISIBLE : View.GONE);
        layoutTabLogs.setVisibility(showKeys ? View.GONE : View.VISIBLE);
        if (btnTabKeys != null) {
            btnTabKeys.setBackgroundResource(showKeys ? R.drawable.bg_tab_active : R.drawable.bg_tab_inactive);
            btnTabKeys.setTextColor(showKeys ? Color.parseColor("#FFFFFF") : Color.parseColor("#8A94A3"));
        }
        if (btnTabLogs != null) {
            btnTabLogs.setBackgroundResource(showKeys ? R.drawable.bg_tab_inactive : R.drawable.bg_tab_active);
            btnTabLogs.setTextColor(showKeys ? Color.parseColor("#8A94A3") : Color.parseColor("#FFFFFF"));
        }
        if (!showKeys) {
            if (tvLogsPlaceholder != null) {
                tvLogsPlaceholder.setVisibility(View.VISIBLE);
                tvLogsPlaceholder.setText("Tap a key in 'Keys' tab to view geo stats.");
            }
        }
    }

    // ── Sync KV ───────────────────────────────────────────────────────────────
    private void syncKeysAndLogs() {
        String token  = prefs.getString("cf_api_token", "");
        String acctId = prefs.getString("cf_account_id", "");
        String kvId   = prefs.getString("cf_kv_namespace_id", "");
        if (token.isEmpty() || acctId.isEmpty() || kvId.isEmpty()) return;
        UiHelper.showMessage(this, "Syncing database…");
        new Thread(() -> {
            try {
                CloudflareApi.Response r = CloudflareApi.get(
                    "/accounts/" + acctId + "/storage/kv/namespaces/" + kvId + "/keys", token);
                runOnUiThread(() -> {
                    try {
                        if (r.isSuccess()) {
                            JSONArray arr = new JSONObject(r.body).getJSONArray("result");
                            renderKeysAndLogs(arr);
                        } else {
                            UiHelper.showError(this, "Sync failed: " + r.code);
                        }
                    } catch (Exception e) {
                        UiHelper.showError(this, "Parse error.");
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> UiHelper.showError(this, "Sync error: " + e.getMessage()));
            }
        }).start();
    }

    private void renderKeysAndLogs(JSONArray keysArray) throws Exception {
        if (containerKeysList == null) return;
        containerKeysList.removeAllViews();
        if (containerLogsList != null) containerLogsList.removeAllViews();

        Set<String> uniqueKeys = new HashSet<>();
        for (int i = 0; i < keysArray.length(); i++) {
            String name = keysArray.getJSONObject(i).getString("name");
            if (name.startsWith("count:") || name.startsWith("devices:"))
                uniqueKeys.add(name.substring(name.indexOf(":") + 1));
        }

        if (!uniqueKeys.isEmpty()) {
            if (tvKeysPlaceholder != null) tvKeysPlaceholder.setVisibility(View.GONE);
            int idx = 0;
            for (final String keyName : uniqueKeys) {
                View row = buildKeyRow(keyName);
                containerKeysList.addView(row);
                if (idx < uniqueKeys.size() - 1) {
                    View div = new View(this);
                    div.setBackgroundColor(0x22888888);
                    containerKeysList.addView(div, new LinearLayout.LayoutParams(-1, 1));
                }
                idx++;
            }
        } else {
            if (tvKeysPlaceholder != null) {
                tvKeysPlaceholder.setVisibility(View.VISIBLE);
                containerKeysList.addView(tvKeysPlaceholder);
            }
        }
        if (tvLogsPlaceholder != null) {
            tvLogsPlaceholder.setVisibility(View.VISIBLE);
            tvLogsPlaceholder.setText("Tap a key in 'Keys' tab to view geo stats.");
            if (containerLogsList != null) containerLogsList.addView(tvLogsPlaceholder);
        }
    }

    private View buildKeyRow(String keyName) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dpToPx(4), dpToPx(14), dpToPx(4), dpToPx(14));
        row.setClickable(true); row.setFocusable(true);
        android.util.TypedValue tv = new android.util.TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true);
        row.setBackgroundResource(tv.resourceId);

        TextView tvName = new TextView(this);
        tvName.setText(keyName);
        tvName.setTextSize(14); tvName.setTypeface(null, android.graphics.Typeface.BOLD);
        tvName.setTextColor(0xFF161B22);
        row.addView(tvName);

        TextView tvSub = new TextView(this);
        tvSub.setText("License key — tap to view details");
        tvSub.setTextSize(11); tvSub.setPadding(0, dpToPx(4), 0, 0);
        tvSub.setTextColor(0xFF5B6472);
        row.addView(tvSub);

        row.setOnClickListener(v -> fetchStatsForAdmin(keyName));
        return row;
    }


    // ── Key Generation ────────────────────────────────────────────────────────
    private void showGenerateKeyDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dpToPx(24), dpToPx(8), dpToPx(24), 0);

        final EditText etName  = new EditText(this); etName.setHint("LICENSE_KEY (e.g., USER-A42)"); etName.setSingleLine(true);
        final EditText etLimit = new EditText(this); etLimit.setHint("Max devices (e.g., 5)"); etLimit.setSingleLine(true);
        etLimit.setInputType(android.text.InputType.TYPE_CLASS_NUMBER); etLimit.setText("5");
        final EditText etDays  = new EditText(this); etDays.setHint("Active days (e.g., 30)"); etDays.setSingleLine(true);
        etDays.setInputType(android.text.InputType.TYPE_CLASS_NUMBER); etDays.setText("30");

        layout.addView(etName); layout.addView(etLimit); layout.addView(etDays);

        new MaterialAlertDialogBuilder(this)
            .setTitle("Generate License Key")
            .setView(layout)
            .setPositiveButton("Create Key", (d, w) -> {
                String key     = etName.getText().toString().trim().toUpperCase();
                String limStr  = etLimit.getText().toString().trim();
                String dayStr  = etDays.getText().toString().trim();
                if (key.isEmpty() || limStr.isEmpty() || dayStr.isEmpty()) {
                    UiHelper.showMessage(this, "All fields are required."); return;
                }
                try {
                    int limit = Integer.parseInt(limStr);
                    int days  = Integer.parseInt(dayStr);
                    if (limit <= 0 || days <= 0) { UiHelper.showMessage(this, "Values must be > 0."); return; }
                    writeLicenseToKV(key, limit, (long) days * 86400L);
                } catch (NumberFormatException ex) {
                    UiHelper.showMessage(this, "Invalid number format.");
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void writeLicenseToKV(String keyName, int maxDevices, long ttlSeconds) {
        String token  = prefs.getString("cf_api_token", "");
        String acctId = prefs.getString("cf_account_id", "");
        String kvId   = prefs.getString("cf_kv_namespace_id", "");
        if (token.isEmpty() || acctId.isEmpty() || kvId.isEmpty()) {
            UiHelper.showMessage(this, "Configure KV namespace first."); return;
        }
        UiHelper.showMessage(this, "Writing license to edge…");
        new Thread(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("max_devices", maxDevices);
                payload.put("registered_devices", new JSONArray());
                CloudflareApi.Response r = CloudflareApi.putJson(
                    "/accounts/" + acctId + "/storage/kv/namespaces/" + kvId
                    + "/values/" + keyName + "?expiration_ttl=" + ttlSeconds,
                    token, payload.toString());
                runOnUiThread(() -> {
                    if (r.isSuccess()) {
                        UiHelper.showMessage(this, "Key '" + keyName + "' created!");
                        syncKeysAndLogs();
                    } else {
                        UiHelper.showError(this, "Failed (" + r.code + ").");
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> UiHelper.showError(this, "Error: " + e.getMessage()));
            }
        }).start();
    }

    // ── Admin Stats / Block ───────────────────────────────────────────────────
    private void fetchStatsForAdmin(String keyName) {
        String workerUrl   = prefs.getString("cf_worker_url", "");
        String adminSecret = prefs.getString("cf_admin_secret", "admin123");
        if (workerUrl.isEmpty()) {
            UiHelper.showMessage(this, "Worker URL not configured."); return;
        }
        UiHelper.showMessage(this, "Loading stats for " + keyName + "…");
        new Thread(() -> {
            try {
                java.net.HttpURLConnection sConn = (java.net.HttpURLConnection) new java.net.URL(
                    workerUrl + "/admin/stats?key=" + keyName).openConnection();
                sConn.setConnectTimeout(15000); sConn.setReadTimeout(15000);
                sConn.setRequestProperty("Authorization", "Bearer " + adminSecret);
                final int sCode = sConn.getResponseCode();
                java.io.InputStream sIn = (sCode >= 200 && sCode < 300) ? sConn.getInputStream() : sConn.getErrorStream();
                java.io.BufferedReader sBr = new java.io.BufferedReader(new java.io.InputStreamReader(sIn, "UTF-8"));
                StringBuilder sSb = new StringBuilder(); String sLn;
                while ((sLn = sBr.readLine()) != null) sSb.append(sLn);
                sConn.disconnect();
                CloudflareApi.Response r = new CloudflareApi.Response(sCode, sSb.toString());
                // Simplified: show raw response in dialog
                final String body = r.body;
                runOnUiThread(() -> showStatsDialog(keyName, body));
            } catch (Exception e) {
                runOnUiThread(() -> showKeyDetailsDialog(keyName));
            }
        }).start();
    }

    private void showStatsDialog(String keyName, String rawData) {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Stats: " + keyName)
            .setMessage(rawData)
            .setPositiveButton("Close", null)
            .setNegativeButton("Delete Key", (d, w) -> confirmDeleteKey(keyName))
            .show();
    }

    private void showKeyDetailsDialog(String keyName) {
        String token  = prefs.getString("cf_api_token", "");
        String acctId = prefs.getString("cf_account_id", "");
        String kvId   = prefs.getString("cf_kv_namespace_id", "");
        if (token.isEmpty() || acctId.isEmpty() || kvId.isEmpty()) return;
        new Thread(() -> {
            try {
                CloudflareApi.Response r = CloudflareApi.get(
                    "/accounts/" + acctId + "/storage/kv/namespaces/" + kvId + "/values/" + keyName, token);
                runOnUiThread(() -> {
                    try {
                        String msg;
                        if (r.isSuccess()) {
                            JSONObject obj = new JSONObject(r.body);
                            int maxD = obj.optInt("max_devices", 5);
                            JSONArray devs = obj.optJSONArray("registered_devices");
                            StringBuilder sb = new StringBuilder("Max Devices: ").append(maxD).append("\n\nRegistered:");
                            if (devs != null && devs.length() > 0)
                                for (int i = 0; i < devs.length(); i++) sb.append("\n• ").append(devs.getString(i));
                            else sb.append("\nNone");
                            msg = sb.toString();
                        } else {
                            msg = "Value: " + r.body;
                        }
                        new MaterialAlertDialogBuilder(this)
                            .setTitle("Key: " + keyName)
                            .setMessage(msg)
                            .setPositiveButton("Close", null)
                            .setNegativeButton("Delete", (d, w) -> confirmDeleteKey(keyName))
                            .show();
                    } catch (Exception ex) {
                        UiHelper.showError(this, "Parse error.");
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> UiHelper.showError(this, "Error: " + e.getMessage()));
            }
        }).start();
    }

    private void confirmDeleteKey(String keyName) {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Delete Key")
            .setMessage("Delete '" + keyName + "' permanently?")
            .setPositiveButton("Delete", (d, w) -> deleteKey(keyName))
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void deleteKey(String keyName) {
        String token  = prefs.getString("cf_api_token", "");
        String acctId = prefs.getString("cf_account_id", "");
        String kvId   = prefs.getString("cf_kv_namespace_id", "");
        new Thread(() -> {
            try {
                CloudflareApi.Response r = CloudflareApi.delete(
                    "/accounts/" + acctId + "/storage/kv/namespaces/" + kvId + "/values/" + keyName, token);
                runOnUiThread(() -> {
                    if (r.isSuccess()) { UiHelper.showMessage(this, "Key deleted."); syncKeysAndLogs(); }
                    else UiHelper.showError(this, "Delete failed: " + r.code);
                });
            } catch (Exception e) {
                runOnUiThread(() -> UiHelper.showError(this, "Error: " + e.getMessage()));
            }
        }).start();
    }

    // ── API Simulation ────────────────────────────────────────────────────────
    private void runAPISimulation() {
        if (etSimUrl == null || etSimKey == null || etSimDevice == null) return;
        String url    = etSimUrl.getText().toString().trim();
        String key    = etSimKey.getText().toString().trim();
        String device = etSimDevice.getText().toString().trim();
        if (url.isEmpty() || key.isEmpty() || device.isEmpty()) {
            UiHelper.showMessage(this, "Fill in all simulation fields."); return;
        }
        if (tvSimResult != null) {
            tvSimResult.setText("Running…");
            tvSimResult.setTextColor(0xFFF38020);
        }
        new Thread(() -> {
            try {
                CloudflareApi.Response r = CloudflareApi.get(
                    url + "/check?key=" + key + "&device=" + device + "&nonce=sim123", "");
                runOnUiThread(() -> {
                    if (tvSimResult != null) {
                        tvSimResult.setTextColor(0xFF5B6472);
                        tvSimResult.setText("HTTP " + r.code + "\n\n" + r.body);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (tvSimResult != null) {
                        tvSimResult.setTextColor(0xFFD6483F);
                        tvSimResult.setText("Error: " + e.getMessage());
                    }
                });
            }
        }).start();
    }

    // ── Maintenance ───────────────────────────────────────────────────────────
    private void showMaintenanceDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dpToPx(24), dpToPx(8), dpToPx(24), dpToPx(8));

        final TextView tvStatus = new TextView(this);
        tvStatus.setText("Loading status…");
        tvStatus.setTextSize(13);
        tvStatus.setPadding(0, 0, 0, dpToPx(12));
        layout.addView(tvStatus);

        final EditText etMsg = new EditText(this);
        etMsg.setHint("Maintenance message (optional)");
        etMsg.setText("Down for maintenance, try again later.");
        layout.addView(etMsg);

        fetchMaintenanceStatus(tvStatus);

        new MaterialAlertDialogBuilder(this)
            .setTitle("Maintenance Mode")
            .setView(layout)
            .setPositiveButton("Activate", (d, w) -> setMaintenanceMode(true, etMsg.getText().toString().trim(), tvStatus))
            .setNegativeButton("Deactivate", (d, w) -> setMaintenanceMode(false, "", tvStatus))
            .setNeutralButton("Close", null)
            .show();
    }

    private void fetchMaintenanceStatus(TextView tvStatus) {
        String workerUrl   = prefs.getString("cf_worker_url", "");
        String adminSecret = prefs.getString("cf_admin_secret", "admin123");
        if (workerUrl.isEmpty()) return;
        new Thread(() -> {
            try {
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(
                    workerUrl + "/admin/maintenance").openConnection();
                conn.setConnectTimeout(15000); conn.setReadTimeout(15000);
                conn.setRequestProperty("Authorization", "Bearer " + adminSecret);
                final int code = conn.getResponseCode();
                java.io.InputStream in2 = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
                java.io.BufferedReader br2 = new java.io.BufferedReader(new java.io.InputStreamReader(in2, "UTF-8"));
                StringBuilder sb2 = new StringBuilder(); String ln;
                while ((ln = br2.readLine()) != null) sb2.append(ln);
                conn.disconnect();
                CloudflareApi.Response r = new CloudflareApi.Response(code, sb2.toString());
                runOnUiThread(() -> {
                    try {
                        JSONObject obj = new JSONObject(r.body);
                        boolean active = obj.optBoolean("active", false);
                        String msg     = obj.optString("message", "");
                        tvStatus.setText(active ? "Status: ACTIVE\nMessage: " + msg : "Status: INACTIVE");
                        tvStatus.setTextColor(active ? 0xFFD6483F : 0xFF1E9E6B);
                    } catch (Exception ex) {
                        tvStatus.setText("Could not load status.");
                    }
                });
            } catch (Exception ignored) {}
        }).start();
    }

    private void setMaintenanceMode(boolean active, String message, TextView tvStatus) {
        String workerUrl   = prefs.getString("cf_worker_url", "");
        String adminSecret = prefs.getString("cf_admin_secret", "admin123");
        if (workerUrl.isEmpty()) { UiHelper.showMessage(this, "Worker URL not set."); return; }
        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("active", active);
                if (!message.isEmpty()) body.put("message", message);
                java.net.HttpURLConnection httpConn = (java.net.HttpURLConnection) new java.net.URL(
                    workerUrl + "/admin/maintenance").openConnection();
                httpConn.setConnectTimeout(15000); httpConn.setReadTimeout(15000);
                httpConn.setDoOutput(true); httpConn.setRequestMethod("POST");
                httpConn.setRequestProperty("Authorization", "Bearer " + adminSecret);
                httpConn.setRequestProperty("Content-Type", "application/json");
                byte[] data = body.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
                httpConn.getOutputStream().write(data);
                final int mCode = httpConn.getResponseCode();
                java.io.InputStream mIn = (mCode >= 200 && mCode < 300) ? httpConn.getInputStream() : httpConn.getErrorStream();
                java.io.BufferedReader mBr = new java.io.BufferedReader(new java.io.InputStreamReader(mIn, "UTF-8"));
                StringBuilder mSb = new StringBuilder(); String mLn;
                while ((mLn = mBr.readLine()) != null) mSb.append(mLn);
                httpConn.disconnect();
                CloudflareApi.Response r = new CloudflareApi.Response(mCode, mSb.toString());
                runOnUiThread(() -> {
                    if (r.isSuccess()) {
                        UiHelper.showMessage(this, active ? "Maintenance ACTIVATED." : "Maintenance DEACTIVATED.");
                        fetchMaintenanceStatus(tvStatus);
                    } else {
                        UiHelper.showError(this, "Failed: " + r.code);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> UiHelper.showError(this, "Error: " + e.getMessage()));
            }
        }).start();
    }

    // ── Scope Error ───────────────────────────────────────────────────────────
    private void handleScopeError(String body) {
        try {
            JSONArray errors = new JSONObject(body).getJSONArray("errors");
            if (errors.length() > 0) {
                String msg = errors.getJSONObject(0).getString("message");
                if (msg.toLowerCase().contains("permission") || msg.toLowerCase().contains("scope")) {
                    new MaterialAlertDialogBuilder(this)
                        .setTitle("Token Permissions Required")
                        .setMessage("Required scopes:\n• Workers Scripts: Edit\n• Workers KV: Edit\n\nError: " + msg)
                        .setPositiveButton("Open Dashboard", (d, w) ->
                            startActivity(new Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://dash.cloudflare.com/profile/api-tokens"))))
                        .setNegativeButton("Close", null)
                        .show();
                    return;
                }
            }
        } catch (Exception ignored) {}
        UiHelper.showError(this, "API error.");
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
