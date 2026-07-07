package com.example.myapo;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

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

import android.widget.TextView;

public class SecundActivity extends AppCompatActivity {

    private TextView tvWorkerName;
    private MaterialButton tvCopyUrl;
    private EditText etCodeEditor;
    private MaterialButton btnSecrets;
    private MaterialButton btnDeploy;
    private Toolbar toolbar;

    private SharedPreferences prefs;
    private String pickedWorkerName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_secund);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.coordinator_editor), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, 0, bars.right, bars.bottom);
            return insets;
        });

        prefs           = getSharedPreferences("mode", MODE_PRIVATE);
        pickedWorkerName = getIntent().getStringExtra("WORKER_NAME");

        bindViews();
        setupToolbar();
        setupListeners();
        fetchWorkerCode();
    }

    private void bindViews() {
        toolbar       = findViewById(R.id.toolbar_editor);
        tvWorkerName  = findViewById(R.id.tv_worker_name);
        tvCopyUrl     = findViewById(R.id.tv_copy_url);
        etCodeEditor  = findViewById(R.id.et_code_editor);
        btnSecrets    = findViewById(R.id.btn_secrets);
        btnDeploy     = findViewById(R.id.btn_deploy);

        if (pickedWorkerName != null)
            tvWorkerName.setText(pickedWorkerName);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Script Editor");
        }
    }

    private void setupListeners() {
        btnDeploy.setOnClickListener(v -> deployWorkerCode());
        tvCopyUrl.setOnClickListener(v -> copyLiveUrl());
        btnSecrets.setOnClickListener(v -> showSecretsManager());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            overridePendingTransition(R.anim.fade_in, R.anim.slide_out_left);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.fade_in, R.anim.slide_out_left);
    }

    // ── Copy URL ──────────────────────────────────────────────────────────────
    private void copyLiveUrl() {
        String sub = prefs.getString("cf_subdomain", "");
        if (sub.isEmpty() || pickedWorkerName == null) {
            UiHelper.showMessage(this, "Subdomain not synced yet."); return;
        }
        String url = "https://" + pickedWorkerName + "." + sub + ".workers.dev";
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("Worker URL", url));
        UiHelper.showMessage(this, "Copied: " + url);
    }

    // ── Secrets Manager ───────────────────────────────────────────────────────
    private void showSecretsManager() {
        String token  = prefs.getString("cf_api_token", "");
        String acctId = prefs.getString("cf_account_id", "");
        if (token.isEmpty() || acctId.isEmpty() || pickedWorkerName == null) {
            UiHelper.showMessage(this, "API config missing."); return;
        }
        UiHelper.showMessage(this, "Loading secrets…");
        new Thread(() -> {
            try {
                CloudflareApi.Response r = CloudflareApi.get(
                    "/accounts/" + acctId + "/workers/scripts/" + pickedWorkerName + "/secrets", token);
                runOnUiThread(() -> {
                    try {
                        if (!r.isSuccess()) {
                            UiHelper.showError(this, "Failed: " + r.code); return;
                        }
                        JSONArray arr = new JSONObject(r.body).getJSONArray("result");
                        displaySecretsList(arr);
                    } catch (Exception e) {
                        UiHelper.showError(this, "Parse error.");
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> UiHelper.showError(this, "Connection failed: " + e.getMessage()));
            }
        }).start();
    }

    private void displaySecretsList(JSONArray arr) throws Exception {
        String[] names = new String[arr.length()];
        for (int i = 0; i < arr.length(); i++)
            names[i] = arr.getJSONObject(i).getString("name");

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Secrets — " + pickedWorkerName);
        if (names.length == 0) {
            builder.setMessage("No secrets uploaded yet.");
        } else {
            builder.setItems(names, (d, which) -> {
                String sel = names[which];
                new MaterialAlertDialogBuilder(this)
                    .setTitle("Delete Secret")
                    .setMessage("Delete '" + sel + "' from secure edge storage?")
                    .setPositiveButton("Delete", (dd, ww) -> deleteSecret(sel))
                    .setNegativeButton("Cancel", null)
                    .show();
            });
        }
        builder.setPositiveButton("Add Secret", (d, w) -> showAddSecretDialog());
        builder.setNegativeButton("Close", null);
        builder.show();
    }

    private void showAddSecretDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dpToPx(24), dpToPx(8), dpToPx(24), 0);

        final EditText etName = new EditText(this);
        etName.setHint("SECRET_NAME");
        etName.setSingleLine(true);
        final EditText etVal = new EditText(this);
        etVal.setHint("Secret value…");
        etVal.setSingleLine(true);

        layout.addView(etName);
        layout.addView(etVal);

        new MaterialAlertDialogBuilder(this)
            .setTitle("Upload Secret")
            .setView(layout)
            .setPositiveButton("Upload", (d, w) -> {
                String name = etName.getText().toString().trim().toUpperCase();
                String val  = etVal.getText().toString().trim();
                if (!name.isEmpty() && !val.isEmpty()) uploadSecret(name, val);
                else UiHelper.showMessage(this, "Both fields are required.");
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void uploadSecret(String name, String value) {
        String token  = prefs.getString("cf_api_token", "");
        String acctId = prefs.getString("cf_account_id", "");
        UiHelper.showMessage(this, "Uploading secret…");
        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("name", name); body.put("text", value); body.put("type", "secret_text");
                CloudflareApi.Response r = CloudflareApi.putJson(
                    "/accounts/" + acctId + "/workers/scripts/" + pickedWorkerName + "/secrets",
                    token, body.toString());
                runOnUiThread(() -> {
                    if (r.isSuccess()) UiHelper.showMessage(this, "Secret '" + name + "' uploaded.");
                    else UiHelper.showError(this, "Upload failed: " + r.code);
                });
            } catch (Exception e) {
                runOnUiThread(() -> UiHelper.showError(this, "Error: " + e.getMessage()));
            }
        }).start();
    }

    private void deleteSecret(String name) {
        String token  = prefs.getString("cf_api_token", "");
        String acctId = prefs.getString("cf_account_id", "");
        new Thread(() -> {
            try {
                CloudflareApi.Response r = CloudflareApi.delete(
                    "/accounts/" + acctId + "/workers/scripts/" + pickedWorkerName + "/secrets/" + name, token);
                runOnUiThread(() -> {
                    if (r.isSuccess()) UiHelper.showMessage(this, "Secret '" + name + "' deleted.");
                    else UiHelper.showError(this, "Delete failed: " + r.code);
                });
            } catch (Exception e) {
                runOnUiThread(() -> UiHelper.showError(this, "Error: " + e.getMessage()));
            }
        }).start();
    }

    // ── Fetch Code ────────────────────────────────────────────────────────────
    private void fetchWorkerCode() {
        String token  = prefs.getString("cf_api_token", "");
        String acctId = prefs.getString("cf_account_id", "");
        if (token.isEmpty() || acctId.isEmpty() || pickedWorkerName == null) {
            UiHelper.showMessage(this, "Configuration missing."); return;
        }
        etCodeEditor.setHint("Downloading script…");
        new Thread(() -> {
            try {
                CloudflareApi.Response r = CloudflareApi.get(
                    "/accounts/" + acctId + "/workers/scripts/" + pickedWorkerName, token);
                runOnUiThread(() -> {
                    if (r.isSuccess()) {
                        etCodeEditor.setText(r.body.trim());
                        etCodeEditor.setSelection(0);
                    } else {
                        UiHelper.showError(this, "Could not load script (" + r.code + ").");
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> UiHelper.showError(this, "Download error: " + e.getMessage()));
            }
        }).start();
    }

    // ── Deploy ────────────────────────────────────────────────────────────────
    private void deployWorkerCode() {
        String token  = prefs.getString("cf_api_token", "");
        String acctId = prefs.getString("cf_account_id", "");
        String code   = etCodeEditor.getText().toString();
        if (token.isEmpty() || acctId.isEmpty() || pickedWorkerName == null) {
            UiHelper.showMessage(this, "Config missing."); return;
        }
        if (code.trim().isEmpty()) { UiHelper.showMessage(this, "Code cannot be empty."); return; }
        UiHelper.showMessage(this, "Deploying to edge…");
        btnDeploy.setEnabled(false);
        new Thread(() -> {
            try {
                CloudflareApi.Response r = CloudflareApi.putScript(
                    "/accounts/" + acctId + "/workers/scripts/" + pickedWorkerName, token, code);
                runOnUiThread(() -> {
                    btnDeploy.setEnabled(true);
                    if (r.isSuccess()) UiHelper.showMessage(this, "Deployed successfully 🚀");
                    else UiHelper.showError(this, "Deploy failed (" + r.code + ").");
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    btnDeploy.setEnabled(true);
                    UiHelper.showError(this, "Network error: " + e.getMessage());
                });
            }
        }).start();
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
