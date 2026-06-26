package com.example.myapo;

import java.net.URL;
import java.net.HttpURLConnection;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import org.json.JSONObject;
import org.json.JSONArray;

import android.app.Activity;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import com.example.myapo.utils.UiHelper;
import android.widget.ScrollView;

@SuppressWarnings("deprecation")
public class LicenseActivity extends Activity {
	
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
	
	private Button btnTabKeys;
	private Button btnTabLogs;
	private Button btnGenerateKey;
	private Button btnCreateNamespace;
	private Button btnRunSim;
	
	private SharedPreferences mode;
	private ProgressDialog progressDialog;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_license);
		initialize();
		initializeLogic();
	}

	private void initialize() {
    linearLicenseBg = findViewById(getFnmods("linear_license_bg","id"));
    cardKvSetup = findViewById(getFnmods("card_kv_setup","id"));
    cardSimulator = findViewById(getFnmods("card_simulator","id"));
    layoutTabKeys = findViewById(getFnmods("layout_tab_keys","id"));
    layoutTabLogs = findViewById(getFnmods("layout_tab_logs","id"));
    containerKeysList = findViewById(getFnmods("container_keys_list","id"));
    containerLogsList = findViewById(getFnmods("container_logs_list","id"));
    
    tvKvNamespaceLabel = findViewById(getFnmods("tv_kv_namespace_label","id"));
    tvKvNamespaceId = findViewById(getFnmods("tv_kv_namespace_id","id"));
    tvKeysPlaceholder = findViewById(getFnmods("tv_keys_placeholder","id"));
    tvLogsPlaceholder = findViewById(getFnmods("tv_logs_placeholder","id"));
    tvSimulatorTitle = findViewById(getFnmods("tv_simulator_title","id"));
    tvSimulatorDesc = findViewById(getFnmods("tv_simulator_desc","id"));
    tvSimResult = findViewById(getFnmods("tv_sim_result","id"));
    
    etSimUrl = findViewById(getFnmods("et_sim_url","id"));
    etSimKey = findViewById(getFnmods("et_sim_key","id"));
    etSimDevice = findViewById(getFnmods("et_sim_device","id"));
    
    btnTabKeys = findViewById(getFnmods("btn_tab_keys","id"));
    btnTabLogs = findViewById(getFnmods("btn_tab_logs","id"));
    btnGenerateKey = findViewById(getFnmods("btn_generate_key","id"));
    btnCreateNamespace = findViewById(getFnmods("btn_create_namespace","id"));
    btnRunSim = findViewById(getFnmods("btn_run_sim","id"));
    
    mode = getSharedPreferences("mode", Activity.MODE_PRIVATE);
    
    ActionBar actionBar = getActionBar();
    if (actionBar != null) {
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle("Licensing Console");
    }

    tvKvNamespaceId.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            showNamespaceSetupDialog();
        }
    });

    btnTabKeys.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switchTab(true);
        }
    });

    btnTabLogs.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switchTab(false);
        }
    });

    btnGenerateKey.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            showGenerateKeyDialog();
        }
    });

    btnCreateNamespace.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            showCreateNamespaceDialog();
        }
    });

    btnRunSim.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            runAPIVerificationSimulation();
        }
    });

    // ============================================================
    // 🔥 TOMBOL MAINTENANCE MODE — TAMBAHKAN DI SINI
    // ============================================================
    Button btnMaintenance = new Button(this);
    btnMaintenance.setText("Maintenance mode");
    btnMaintenance.setBackgroundResource(getFnmods("bg_button", "drawable"));
    btnMaintenance.setTextColor(Color.WHITE);
    btnMaintenance.setAllCaps(false);
    btnMaintenance.setTypeface(null, android.graphics.Typeface.BOLD);
    btnMaintenance.setPadding(20, 16, 20, 16);
    btnMaintenance.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            showMaintenanceDialog();
        }
    });
    
    // Tambahkan ke cardKvSetup (atau cardSimulator, terserah)
    // cardKvSetup.addView(btnMaintenance); // UNCOMMENT INI
    // Atau tambahkan di atas btnRunSim
    LinearLayout parent = (LinearLayout) btnRunSim.getParent();
    if (parent != null) {
        parent.addView(btnMaintenance, parent.indexOfChild(btnRunSim));
    }
}
	
	private void initializeLogic() {
		applyTheme();
		updateNamespaceLabel();
		
		String kvId = mode.getString("cf_kv_namespace_id", "");
		if (!kvId.isEmpty()) {
			syncKeysAndLogs();
		}

		String subdomain = mode.getString("cf_subdomain", "");
		if (!subdomain.isEmpty()) {
			etSimUrl.setHint("https://your-script." + subdomain + ".workers.dev");
		}

		if (mode.getString("cf_worker_url", "").isEmpty()) {
			mode.edit().putString("cf_worker_url", "https://mlbb-key-check.ardhiannizar12.workers.dev").commit();
		}
	}

	private void updateNamespaceLabel() {
		String kvId = mode.getString("cf_kv_namespace_id", "");
		if (kvId.isEmpty()) {
			tvKvNamespaceId.setText("Click to set KV Namespace ID...");
		} else {
			tvKvNamespaceId.setText(kvId);
		}
	}

	private void switchTab(boolean showKeys) {
		if (showKeys) {
			layoutTabKeys.setVisibility(View.VISIBLE);
			layoutTabLogs.setVisibility(View.GONE);
			btnTabKeys.setBackgroundResource(getFnmods("bg_tab_active", "drawable"));
			btnTabKeys.setTextColor(Color.parseColor("#0B0F17"));
			btnTabLogs.setBackgroundResource(getFnmods("bg_tab_inactive", "drawable"));
			btnTabLogs.setTextColor(Color.parseColor("#8A94A3"));
		} else {
			layoutTabKeys.setVisibility(View.GONE);
			layoutTabLogs.setVisibility(View.VISIBLE);
			btnTabKeys.setBackgroundResource(getFnmods("bg_tab_inactive", "drawable"));
			btnTabKeys.setTextColor(Color.parseColor("#8A94A3"));
			btnTabLogs.setBackgroundResource(getFnmods("bg_tab_active", "drawable"));
			btnTabLogs.setTextColor(Color.parseColor("#0B0F17"));
			syncLogsOnly();
		}
	}
	
	private void showMaintenanceDialog() {
    LinearLayout layout = new LinearLayout(this);
    layout.setOrientation(LinearLayout.VERTICAL);
    layout.setPadding(20, 20, 20, 20);

    // Status saat ini
    final TextView tvStatus = new TextView(this);
    tvStatus.setText("Loading status...");
    tvStatus.setTextColor(Color.WHITE);
    tvStatus.setPadding(0, 0, 0, 16);
    layout.addView(tvStatus);

    final EditText etMessage = new EditText(this);
    etMessage.setHint("Pesan maintenance (opsional)");
    etMessage.setText("Sedang maintenance, coba lagi nanti.");
    layout.addView(etMessage);

    LinearLayout btnRow = new LinearLayout(this);
    btnRow.setOrientation(LinearLayout.HORIZONTAL);

    final Button btnActivate = new Button(this);
    btnActivate.setText("Aktifkan");
    btnActivate.setBackgroundResource(getFnmods("bg_status_offline_solid", "drawable"));
    btnActivate.setTextColor(Color.WHITE);
    btnActivate.setAllCaps(false);
    btnActivate.setTypeface(null, android.graphics.Typeface.BOLD);
    btnActivate.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
    btnActivate.setPadding(12, 12, 12, 12);

    final Button btnDeactivate = new Button(this);
    btnDeactivate.setText("Nonaktifkan");
    btnDeactivate.setBackgroundResource(getFnmods("bg_status_online_solid", "drawable"));
    btnDeactivate.setTextColor(Color.WHITE);
    btnDeactivate.setAllCaps(false);
    btnDeactivate.setTypeface(null, android.graphics.Typeface.BOLD);
    btnDeactivate.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
    btnDeactivate.setPadding(12, 12, 12, 12);

    btnRow.addView(btnActivate);
    btnRow.addView(btnDeactivate);
    layout.addView(btnRow);

    final AlertDialog dialog = new AlertDialog.Builder(this)
        .setTitle("Maintenance mode")
        .setView(layout)
        .setNegativeButton("Tutup", null)
        .create();

    // Ambil status awal
    fetchMaintenanceStatus(tvStatus);

    btnActivate.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            setMaintenanceMode(true, etMessage.getText().toString().trim(), dialog, tvStatus);
        }
    });

    btnDeactivate.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            setMaintenanceMode(false, "", dialog, tvStatus);
        }
    });

    dialog.show();
}

private void fetchMaintenanceStatus(final TextView tvStatus) {
    final String workerUrl = mode.getString("cf_worker_url", "");
    final String adminSecret = mode.getString("cf_admin_secret", "admin123");  // ← TAMBAHKAN INI
    if (workerUrl.isEmpty()) return;

    new Thread(new Runnable() {
        @Override
        public void run() {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(workerUrl + "/admin/maintenance");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + adminSecret);

                final int code = conn.getResponseCode();
                if (code >= 200 && code < 300) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    final String data = sb.toString();
                    reader.close();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                JSONObject obj = new JSONObject(data);
                                boolean active = obj.optBoolean("active", false);
                                String msg = obj.optString("message", "");
                                tvStatus.setText(active ? "Status: AKTIF\nPesan: " + msg : "Status: NONAKTIF");
                                tvStatus.setTextColor(active ? Color.parseColor("#D6483F") : Color.parseColor("#1E9E6B"));
                            } catch (Exception e) {
                                tvStatus.setText("Gagal load status");
                            }
                        }
                    });
                }
            } catch (Exception ignored) {
            } finally {
                if (conn != null) conn.disconnect();
            }
        }
    }).start();
}
private void setMaintenanceMode(final boolean active, final String message, final AlertDialog dialog, final TextView tvStatus) {
    final String workerUrl = mode.getString("cf_worker_url", "");
    final String adminSecret = mode.getString("cf_admin_secret", "admin123");
    if (workerUrl.isEmpty()) {
        UiHelper.showMessage(this, "Worker URL belum diatur.");
        return;
    }

    progressDialog = new ProgressDialog(this);
    progressDialog.setMessage(active ? "Mengaktifkan maintenance..." : "Menonaktifkan maintenance...");
    progressDialog.show();

    new Thread(new Runnable() {
        @Override
        public void run() {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(workerUrl + "/admin/maintenance");
                conn = (HttpURLConnection) url.openConnection();
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + adminSecret);
                conn.setRequestProperty("Content-Type", "application/json");

                JSONObject body = new JSONObject();
                body.put("active", active);
                if (!message.isEmpty()) body.put("message", message);

                OutputStream out = conn.getOutputStream();
                out.write(body.toString().getBytes("UTF-8"));
                out.flush();
                out.close();

                final int code = conn.getResponseCode();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (progressDialog != null) progressDialog.dismiss();
                        if (code >= 200 && code < 300) {
                            UiHelper.showMessage(LicenseActivity.this, active ? "✅ Maintenance AKTIF!" : "✅ Maintenance NONAKTIF!");
                            if (dialog != null && dialog.isShowing()) dialog.dismiss();
                            if (tvStatus != null) fetchMaintenanceStatus(tvStatus);
                        } else {
                            UiHelper.showError(LicenseActivity.this, "Gagal: " + code);
                        }
                    }
                });

            } catch (final Exception e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (progressDialog != null) progressDialog.dismiss();
                        UiHelper.showError(LicenseActivity.this, "Error: " + e.getMessage());
                    }
                });
            } finally {
                if (conn != null) conn.disconnect();
            }
        }
    }).start();
}

	private void showNamespaceSetupDialog() {
		final EditText input = new EditText(this);
		input.setHint("Paste Cloudflare KV Namespace ID...");
		input.setSingleLine(true);
		
		String currentKvId = mode.getString("cf_kv_namespace_id", "");
		if (!currentKvId.isEmpty()) {
			input.setText(currentKvId);
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Database Setup");
		builder.setMessage("Enter the KV Namespace ID dedicated for your keys & log storage.");
		builder.setView(input);
		builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String newKvId = input.getText().toString().trim();
				if (newKvId.isEmpty()) {
					UiHelper.showMessage(LicenseActivity.this, "Namespace ID cannot be empty!");
					return;
				}
				mode.edit().putString("cf_kv_namespace_id", newKvId).commit();
				updateNamespaceLabel();
				syncKeysAndLogs();
			}
		});
		builder.setNegativeButton("Cancel", null);
		builder.show();
	}

	private void showCreateNamespaceDialog() {
		final EditText input = new EditText(this);
		input.setHint("Namespace Name (e.g. app_licenses)");
		input.setSingleLine(true);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Create KV Namespace");
		builder.setMessage("This will create a new Database table in your Cloudflare account automatically via REST API.");
		builder.setView(input);
		builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String name = input.getText().toString().trim().replaceAll(" ", "_");
				if (!name.isEmpty()) {
					createKVNamespaceOnCloudflare(name);
				} else {
					UiHelper.showMessage(LicenseActivity.this, "Name cannot be empty!");
				}
			}
		});
		builder.setNegativeButton("Cancel", null);
		builder.show();
	}

	private void createKVNamespaceOnCloudflare(final String namespaceName) {
		final String token = mode.getString("cf_api_token", "");
		final String accountId = mode.getString("cf_account_id", "");
		
		if (token.isEmpty() || accountId.isEmpty()) {
			UiHelper.showMessage(this, "API configuration details missing.");
			return;
		}

		progressDialog = new ProgressDialog(this);
		progressDialog.setMessage("Creating database table...");
		progressDialog.setCancelable(false);
		progressDialog.show();

		new Thread(new Runnable() {
			@Override
			public void run() {
				HttpURLConnection conn = null;
				BufferedReader reader = null;
				try {
					URL url = new URL("https://api.cloudflare.com/client/v4/accounts/" + accountId + "/storage/kv/namespaces");
					conn = (HttpURLConnection) url.openConnection();
					conn.setDoOutput(true);
					conn.setRequestMethod("POST");
					conn.setRequestProperty("Authorization", "Bearer " + token);
					conn.setRequestProperty("Content-Type", "application/json");

					JSONObject body = new JSONObject();
					body.put("title", namespaceName);

					OutputStream out = conn.getOutputStream();
					out.write(body.toString().getBytes("UTF-8"));
					out.flush();
					out.close();

					final int code = conn.getResponseCode();
					InputStream in = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
					reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
					StringBuilder sb = new StringBuilder();
					String line;
					while ((line = reader.readLine()) != null) sb.append(line);
					final String responseData = sb.toString();

					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (progressDialog != null) progressDialog.dismiss();
							if (code >= 200 && code < 300) {
								try {
									JSONObject parsed = new JSONObject(responseData);
									JSONObject res = parsed.getJSONObject("result");
									String generatedId = res.getString("id");
									mode.edit().putString("cf_kv_namespace_id", generatedId).commit();
									updateNamespaceLabel();
									UiHelper.showMessage(LicenseActivity.this, "Database KV created & configured!");
									syncKeysAndLogs();
								} catch (Exception e) {
									UiHelper.showMessage(LicenseActivity.this, "Database created! Response parsed error.");
								}
							} else {
								UiHelper.showError(LicenseActivity.this, "Failed creation (" + code + "): " + responseData);
							}
						}
					});

				} catch (Exception e) {
					final Exception finalE = e;
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (progressDialog != null) progressDialog.dismiss();
							UiHelper.showError(LicenseActivity.this, "Creation failed: " + finalE.getMessage());
						}
					});
				} finally {
					if (reader != null) {
						try { reader.close(); } catch (Exception ignored) {}
					}
					if (conn != null) conn.disconnect();
				}
			}
		}).start();
	}

	private void runAPIVerificationSimulation() {
		final String targetUrl = etSimUrl.getText().toString().trim();
		final String targetKey = etSimKey.getText().toString().trim();
		final String targetDevice = etSimDevice.getText().toString().trim();

		if (targetUrl.isEmpty() || targetKey.isEmpty() || targetDevice.isEmpty()) {
			UiHelper.showMessage(this, "Please enter all simulation variables first.");
			return;
		}

		tvSimResult.setText("Executing GET request...");
		tvSimResult.setTextColor(Color.parseColor("#F38020"));

		new Thread(new Runnable() {
			@Override
			public void run() {
				HttpURLConnection conn = null;
				BufferedReader reader = null;
				try {
					String fullUrl = targetUrl + "/check?key=" + targetKey + "&device=" + targetDevice + "&nonce=sim123";
					URL url = new URL(fullUrl);
					conn = (HttpURLConnection) url.openConnection();
					conn.setRequestMethod("GET");
					conn.setRequestProperty("Content-Type", "application/json");

					final int code = conn.getResponseCode();
					InputStream in = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
					reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
					StringBuilder sb = new StringBuilder();
					String line;
					while ((line = reader.readLine()) != null) sb.append(line);
					final String responseData = sb.toString();

					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							boolean isNight = mode.getString("night", "").equals("true");
							tvSimResult.setTextColor(isNight ? Color.parseColor("#8A94A3") : Color.parseColor("#5B6472"));
							tvSimResult.setText("Status Code: " + code + "\n\nResponse:\n" + responseData);
						}
					});

				} catch (Exception e) {
					final Exception finalE = e;
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							tvSimResult.setTextColor(Color.parseColor("#D6483F"));
							tvSimResult.setText("Error: " + finalE.getMessage());
						}
					});
				} finally {
					if (reader != null) {
						try { reader.close(); } catch (Exception ignored) {}
					}
					if (conn != null) conn.disconnect();
				}
			}
		}).start();
	}

	private void applyTheme() {
		boolean isNight = mode.getString("night", "").equals("true");
		ActionBar actionBar = getActionBar();
		float density = getResources().getDisplayMetrics().density;

		int cardBg = isNight ? Color.parseColor("#141B26") : Color.parseColor("#FFFFFF");
		int cardStroke = isNight ? Color.parseColor("#2A3441") : Color.parseColor("#E2E5EA");
		int mainBg = isNight ? Color.parseColor("#0B0F17") : Color.parseColor("#F4F5F7");

		linearLicenseBg.setBackgroundColor(mainBg);

		GradientDrawable gdSetup = new GradientDrawable();
		gdSetup.setColor(cardBg);
		gdSetup.setCornerRadius(18 * density);
		gdSetup.setStroke((int) (1 * density), cardStroke);
		cardKvSetup.setBackground(gdSetup);

		GradientDrawable gdKeys = new GradientDrawable();
		gdKeys.setColor(cardBg);
		gdKeys.setCornerRadius(18 * density);
		gdKeys.setStroke((int) (1 * density), cardStroke);
		containerKeysList.setBackground(gdKeys);

		GradientDrawable gdLogs = new GradientDrawable();
		gdLogs.setColor(cardBg);
		gdLogs.setCornerRadius(18 * density);
		gdLogs.setStroke((int) (1 * density), cardStroke);
		containerLogsList.setBackground(gdLogs);

		GradientDrawable gdSim = new GradientDrawable();
		gdSim.setColor(cardBg);
		gdSim.setCornerRadius(18 * density);
		gdSim.setStroke((int) (1 * density), cardStroke);
		cardSimulator.setBackground(gdSim);

		int primaryColor = isNight ? Color.parseColor("#E8EAED") : Color.parseColor("#161B22");
		int secondaryColor = isNight ? Color.parseColor("#8A94A3") : Color.parseColor("#5B6472");

		tvKvNamespaceLabel.setTextColor(primaryColor);
		tvKeysPlaceholder.setTextColor(secondaryColor);
		tvLogsPlaceholder.setTextColor(secondaryColor);
		tvSimulatorTitle.setTextColor(primaryColor);
		tvSimulatorDesc.setTextColor(secondaryColor);
		etSimUrl.setTextColor(primaryColor);
		etSimUrl.setHintTextColor(secondaryColor);
		etSimKey.setTextColor(primaryColor);
		etSimKey.setHintTextColor(secondaryColor);
		etSimDevice.setTextColor(primaryColor);
		etSimDevice.setHintTextColor(secondaryColor);
		tvSimResult.setTextColor(secondaryColor);

		if (actionBar != null) {
			actionBar.setBackgroundDrawable(new ColorDrawable(isNight ? Color.parseColor("#0E1420") : Color.parseColor("#F38020")));
		}
	}

	private void syncKeysAndLogs() {
		final String token = mode.getString("cf_api_token", "");
		final String accountId = mode.getString("cf_account_id", "");
		final String kvId = mode.getString("cf_kv_namespace_id", "");
		
		if (token.isEmpty() || accountId.isEmpty() || kvId.isEmpty()) {
			return;
		}

		progressDialog = new ProgressDialog(this);
		progressDialog.setMessage("Synchronizing database...");
		progressDialog.setCancelable(false);
		progressDialog.show();

		new Thread(new Runnable() {
			@Override
			public void run() {
				HttpURLConnection conn = null;
				BufferedReader reader = null;
				try {
					URL url = new URL("https://api.cloudflare.com/client/v4/accounts/" + accountId + "/storage/kv/namespaces/" + kvId + "/keys");
					conn = (HttpURLConnection) url.openConnection();
					conn.setRequestMethod("GET");
					conn.setRequestProperty("Authorization", "Bearer " + token);

					final int code = conn.getResponseCode();
					InputStream in = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
					reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
					StringBuilder sb = new StringBuilder();
					String line;
					while ((line = reader.readLine()) != null) {
						sb.append(line);
					}
					final String responseData = sb.toString();

					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (progressDialog != null) progressDialog.dismiss();
							if (code >= 200 && code < 300) {
								try {
									JSONObject obj = new JSONObject(responseData);
									JSONArray arr = obj.getJSONArray("result");
									renderKeysAndLogs(arr);
								} catch (Exception e) {
									UiHelper.showMessage(LicenseActivity.this, "Failed to parse database metadata.");
								}
							} else {
								UiHelper.showError(LicenseActivity.this, "Sync failed: " + responseData);
							}
						}
					});

				} catch (Exception e) {
					final Exception finalE = e;
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (progressDialog != null) progressDialog.dismiss();
							UiHelper.showError(LicenseActivity.this, "Sync Error: " + finalE.getMessage());
						}
					});
				} finally {
					if (reader != null) {
						try { reader.close(); } catch (Exception ignored) {}
					}
					if (conn != null) conn.disconnect();
				}
			}
		}).start();
	}

	private void syncLogsOnly() {
		final String kvId = mode.getString("cf_kv_namespace_id", "");
		final String workerUrl = mode.getString("cf_worker_url", "");

		if (kvId.isEmpty() || workerUrl.isEmpty()) {
			UiHelper.showMessage(this, "Set KV Namespace ID & Worker URL first.");
			return;
		}

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				tvLogsPlaceholder.setVisibility(View.VISIBLE);
				tvLogsPlaceholder.setText("Tap a key in 'Keys List' to see GEO stats.");
			}
		});
	}

	private void renderKeysAndLogs(JSONArray keysArray) throws Exception {
    containerKeysList.removeAllViews();
    containerLogsList.removeAllViews();
    
    boolean isNight = mode.getString("night", "").equals("true");
    int primaryTextColor = isNight ? Color.parseColor("#E8EAED") : Color.parseColor("#161B22");
    int secondaryTextColor = isNight ? Color.parseColor("#8A94A3") : Color.parseColor("#5B6472");
    int dividerColor = isNight ? Color.parseColor("#2A3441") : Color.parseColor("#E2E5EA");

    // 🔥 Kumpulkan key unik dari prefix count: dan devices:
    java.util.Set<String> uniqueKeys = new java.util.HashSet<>();
    JSONArray logsOnly = new JSONArray();

    for (int i = 0; i < keysArray.length(); i++) {
        JSONObject item = keysArray.getJSONObject(i);
        String name = item.getString("name");
        if (name.startsWith("count:") || name.startsWith("devices:")) {
            // Ekstrak nama key asli (hapus prefix)
            String key = name.substring(name.indexOf(":") + 1);
            uniqueKeys.add(key);
        } else if (name.startsWith("log:")) {
            logsOnly.put(item);
        }
    }

    // ── RENDER LICENSE KEYS (uniqueKeys) ──
    if (!uniqueKeys.isEmpty()) {
        tvKeysPlaceholder.setVisibility(View.GONE);
        int index = 0;
        for (final String keyName : uniqueKeys) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(12, 16, 12, 16);
            row.setClickable(true);
            row.setFocusable(true);
            
            android.util.TypedValue outValue = new android.util.TypedValue();
            getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
            row.setBackgroundResource(outValue.resourceId);
            
            TextView tvName = new TextView(this);
            tvName.setText(keyName);
            tvName.setTextSize(15);
            tvName.setTypeface(null, android.graphics.Typeface.BOLD);
            tvName.setTextColor(primaryTextColor);
            row.addView(tvName);
            
            // Tampilkan jumlah device (opsional, bisa ambil dari KV count)
            String countStr = "License key";
            TextView tvSub = new TextView(this);
            tvSub.setText(countStr);
            tvSub.setTextSize(11);
            tvSub.setPadding(0, 4, 0, 0);
            tvSub.setTextColor(secondaryTextColor);
            row.addView(tvSub);

            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Langsung tampilkan stats untuk key ini
                    fetchStatsForAdmin(keyName);
                }
            });
            
            containerKeysList.addView(row);
            
            if (index < uniqueKeys.size() - 1) {
                View div = new View(this);
                div.setBackgroundColor(dividerColor);
                containerKeysList.addView(div, -1, 2);
            }
            index++;
        }
    } else {
        tvKeysPlaceholder.setVisibility(View.VISIBLE);
        containerKeysList.addView(tvKeysPlaceholder);
    }

    // ── RENDER LOGS SECTION (tetap pakai placeholder) ──
    renderLogsSection(logsOnly);
}

	private void renderLogsSection(JSONArray logsOnly) throws Exception {
		containerLogsList.removeAllViews();
		// Log format is now log:KEY:DEVICE with a JSON value (country/ip/timestamp),
		// which can't be parsed from the key name alone anymore.
		// GEO stats are now fetched on-demand per key via fetchStatsForAdmin().
		tvLogsPlaceholder.setVisibility(View.VISIBLE);
		tvLogsPlaceholder.setText("Tap a key in 'Keys List' to see GEO stats.");
		containerLogsList.addView(tvLogsPlaceholder);
	}

	private void showKeyDetailsAndOptions(final String keyName) {
		final String token = mode.getString("cf_api_token", "");
		final String accountId = mode.getString("cf_account_id", "");
		final String kvId = mode.getString("cf_kv_namespace_id", "");

		progressDialog = new ProgressDialog(this);
		progressDialog.setMessage("Fetching key payload...");
		progressDialog.setCancelable(false);
		progressDialog.show();

		new Thread(new Runnable() {
			@Override
			public void run() {
				HttpURLConnection conn = null;
				BufferedReader reader = null;
				try {
					URL url = new URL("https://api.cloudflare.com/client/v4/accounts/" + accountId + "/storage/kv/namespaces/" + kvId + "/values/" + keyName);
					conn = (HttpURLConnection) url.openConnection();
					conn.setRequestMethod("GET");
					conn.setRequestProperty("Authorization", "Bearer " + token);

					final int code = conn.getResponseCode();
					InputStream in = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
					reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
					StringBuilder sb = new StringBuilder();
					String line;
					while ((line = reader.readLine()) != null) sb.append(line);
					final String responseData = sb.toString();

					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (progressDialog != null) progressDialog.dismiss();
							if (code >= 200 && code < 300) {
								try {
									JSONObject valObj = new JSONObject(responseData);
									int maxDevices = valObj.optInt("max_devices", 5);
									JSONArray devices = valObj.optJSONArray("registered_devices");
									String registeredListStr = "";
									if (devices != null && devices.length() > 0) {
										for (int i=0; i<devices.length(); i++) {
											registeredListStr += "\n- " + devices.getString(i);
										}
									} else {
										registeredListStr = "\nNo devices registered yet.";
									}

									AlertDialog.Builder builder = new AlertDialog.Builder(LicenseActivity.this);
									builder.setTitle("Key: " + keyName);
									builder.setMessage("Max Devices Limit: " + maxDevices + "\n\nRegistered Hardware IDs:" + registeredListStr);
									builder.setPositiveButton("Close", null);
									builder.setNeutralButton("Stats / Block", new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialog, int which) {
											fetchStatsForAdmin(keyName);
										}
									});
									builder.setNegativeButton("Delete Key", new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialog, int which) {
											confirmDeleteKey(keyName);
										}
									});
									builder.show();
								} catch (Exception e) {
									AlertDialog.Builder builder = new AlertDialog.Builder(LicenseActivity.this);
									builder.setTitle("Key: " + keyName);
									builder.setMessage("Plain Text Value:\n" + responseData);
									builder.setPositiveButton("Close", null);
									builder.setNeutralButton("Stats / Block", new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialog, int which) {
											fetchStatsForAdmin(keyName);
										}
									});
									builder.setNegativeButton("Delete Key", new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialog, int which) {
											confirmDeleteKey(keyName);
										}
									});
									builder.show();
								}
							} else {
								UiHelper.showMessage(LicenseActivity.this, "Failed to load payload.");
							}
						}
					});

				} catch (Exception e) {
					final Exception finalE = e;
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (progressDialog != null) progressDialog.dismiss();
							UiHelper.showError(LicenseActivity.this, "Connection Error: " + finalE.getMessage());
						}
					});
				} finally {
					if (reader != null) {
						try { reader.close(); } catch (Exception ignored) {}
					}
					if (conn != null) conn.disconnect();
				}
			}
		}).start();
	}

	private void confirmDeleteKey(final String keyName) {
		new AlertDialog.Builder(this)
			.setTitle("Confirm Deletion")
			.setMessage("Are you sure you want to delete '" + keyName + "'? This action is irreversible.")
			.setPositiveButton("Yes, Delete", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					deleteKeyFromDatabase(keyName);
				}
			})
			.setNegativeButton("Cancel", null)
			.show();
	}

	private void deleteKeyFromDatabase(final String keyName) {
		final String token = mode.getString("cf_api_token", "");
		final String accountId = mode.getString("cf_account_id", "");
		final String kvId = mode.getString("cf_kv_namespace_id", "");

		progressDialog = new ProgressDialog(this);
		progressDialog.setMessage("Deleting key from edge...");
		progressDialog.setCancelable(false);
		progressDialog.show();

		new Thread(new Runnable() {
			@Override
			public void run() {
				HttpURLConnection conn = null;
				BufferedReader reader = null;
				try {
					URL url = new URL("https://api.cloudflare.com/client/v4/accounts/" + accountId + "/storage/kv/namespaces/" + kvId + "/values/" + keyName);
					conn = (HttpURLConnection) url.openConnection();
					conn.setRequestMethod("DELETE");
					conn.setRequestProperty("Authorization", "Bearer " + token);

					final int code = conn.getResponseCode();
					InputStream in = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
					reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
					StringBuilder sb = new StringBuilder();
					String line;
					while ((line = reader.readLine()) != null) sb.append(line);
					final String responseData = sb.toString();

					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (progressDialog != null) progressDialog.dismiss();
							if (code >= 200 && code < 300) {
								UiHelper.showMessage(LicenseActivity.this, "Deleted successfully!");
								syncKeysAndLogs();
							} else {
								UiHelper.showError(LicenseActivity.this, "Failed deletion: " + responseData);
							}
						}
					});

				} catch (Exception e) {
					final Exception finalE = e;
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (progressDialog != null) progressDialog.dismiss();
							UiHelper.showError(LicenseActivity.this, "Connection failed: " + finalE.getMessage());
						}
					});
				} finally {
					if (reader != null) {
						try { reader.close(); } catch (Exception ignored) {}
					}
					if (conn != null) conn.disconnect();
				}
			}
		}).start();
	}

	private void fetchStatsForAdmin(final String keyName) {
		final String workerUrl = mode.getString("cf_worker_url", "");
		final String adminSecret = mode.getString("cf_admin_secret", "admin123");
		if (workerUrl.isEmpty()) {
			UiHelper.showMessage(this, "Worker URL belum diatur.");
			return;
		}

		progressDialog = new ProgressDialog(this);
		progressDialog.setMessage("Fetching GEO stats...");
		progressDialog.setCancelable(false);
		progressDialog.show();

		new Thread(new Runnable() {
			@Override
			public void run() {
				HttpURLConnection conn = null;
				BufferedReader reader = null;
				try {
					String urlStr = workerUrl + "/admin/stats?key=" + keyName;
					URL url = new URL(urlStr);
					conn = (HttpURLConnection) url.openConnection();
					conn.setRequestMethod("GET");
					conn.setRequestProperty("Authorization", "Bearer " + adminSecret);

					final int code = conn.getResponseCode();
					InputStream in = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
					reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
					StringBuilder sb = new StringBuilder();
					String line;
					while ((line = reader.readLine()) != null) sb.append(line);
					final String responseData = sb.toString();

					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (progressDialog != null) progressDialog.dismiss();
							if (code >= 200 && code < 300) {
								try {
									JSONObject obj = new JSONObject(responseData);
									JSONArray devices = obj.optJSONArray("devices");
									if (devices == null) devices = new JSONArray();
									showStatsDialog(keyName, devices);
								} catch (Exception e) {
									UiHelper.showMessage(LicenseActivity.this, "Error parsing: " + e.getMessage());
								}
							} else {
								UiHelper.showError(LicenseActivity.this, "Failed (" + code + "): " + responseData);
							}
						}
					});

				} catch (Exception e) {
					final Exception finalE = e;
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (progressDialog != null) progressDialog.dismiss();
							UiHelper.showError(LicenseActivity.this, "Error: " + finalE.getMessage());
						}
					});
				} finally {
					if (reader != null) { try { reader.close(); } catch (Exception ignored) {} }
					if (conn != null) conn.disconnect();
				}
			}
		}).start();
	}

	private void showStatsDialog(final String keyName, final JSONArray devicesArray) {
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(20, 20, 20, 20);
    
    TextView header = new TextView(this);
    header.setText(keyName);
    header.setTextSize(16);
    header.setTypeface(null, android.graphics.Typeface.BOLD);
    header.setTextColor(Color.parseColor("#F38020"));
    header.setPadding(0, 0, 0, 2);
    root.addView(header);

    TextView countCaption = new TextView(this);
    countCaption.setText(devicesArray.length() + " device" + (devicesArray.length() == 1 ? "" : "s") + " terdaftar");
    countCaption.setTextSize(11);
    countCaption.setTextColor(Color.parseColor("#8A94A3"));
    countCaption.setPadding(0, 0, 0, 14);
    root.addView(countCaption);

    ScrollView scrollView = new ScrollView(this);
    scrollView.setLayoutParams(new LinearLayout.LayoutParams(-1, 400));
    LinearLayout deviceContainer = new LinearLayout(this);
    deviceContainer.setOrientation(LinearLayout.VERTICAL);
    deviceContainer.setPadding(0, 0, 0, 16);

    final float density = getResources().getDisplayMetrics().density;

    if (devicesArray.length() == 0) {
        TextView empty = new TextView(this);
        empty.setText("Belum ada device yang pakai key ini.");
        empty.setTextColor(Color.parseColor("#8A94A3"));
        empty.setPadding(0, 20, 0, 20);
        deviceContainer.addView(empty);
    } else {
        for (int i = 0; i < devicesArray.length(); i++) {
            try {
                JSONObject d = devicesArray.getJSONObject(i);
                final String deviceId = d.getString("device");
                String country = d.optString("country", "?");
                String ip = d.optString("ip", "?");
                long first = d.optLong("first_used", 0);
                long last = d.optLong("last_used", 0);
                boolean isBlocked = d.optBoolean("blocked", false);

                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setPadding(16, 14, 16, 14);
                LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(-1, -2);
                cardLp.setMargins(0, 0, 0, 10);
                card.setLayoutParams(cardLp);
                
                GradientDrawable gd = new GradientDrawable();
                gd.setColor(isBlocked ? Color.parseColor("#241616") : Color.parseColor("#1C2530"));
                gd.setCornerRadius(14 * density);
                gd.setStroke((int) (1 * density), isBlocked ? Color.parseColor("#D6483F") : Color.parseColor("#2A3441"));
                card.setBackground(gd);

                LinearLayout row1 = new LinearLayout(this);
                row1.setOrientation(LinearLayout.HORIZONTAL);
                row1.setGravity(android.view.Gravity.CENTER_VERTICAL);
                
                TextView tvDevice = new TextView(this);
                tvDevice.setText(deviceId);
                tvDevice.setTextSize(13);
                tvDevice.setTypeface(null, android.graphics.Typeface.BOLD);
                tvDevice.setTextColor(isBlocked ? Color.parseColor("#FF6B61") : Color.parseColor("#E8EAED"));
                tvDevice.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
                row1.addView(tvDevice);
                
                TextView tvStatus = new TextView(this);
                tvStatus.setText(isBlocked ? "BLOCKED" : "ACTIVE");
                tvStatus.setTextSize(10);
                tvStatus.setTypeface(null, android.graphics.Typeface.BOLD);
                tvStatus.setTextColor(isBlocked ? Color.parseColor("#FF6B61") : Color.parseColor("#3DDC97"));
                tvStatus.setPadding(10, 4, 10, 4);
                GradientDrawable statusBg = new GradientDrawable();
                statusBg.setCornerRadius(100 * density);
                statusBg.setColor(isBlocked ? Color.parseColor("#3A1E1E") : Color.parseColor("#15302A"));
                tvStatus.setBackground(statusBg);
                row1.addView(tvStatus);
                card.addView(row1);

                TextView tvGeo = new TextView(this);
                tvGeo.setText(country + "  •  " + ip);
                tvGeo.setTextSize(11);
                tvGeo.setTypeface(android.graphics.Typeface.MONOSPACE);
                tvGeo.setTextColor(Color.parseColor("#8A94A3"));
                tvGeo.setPadding(0, 6, 0, 0);
                card.addView(tvGeo);

                TextView tvTime = new TextView(this);
                if (first > 0) {
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.US);
                    String firstStr = sdf.format(new java.util.Date(first * 1000));
                    String lastStr = sdf.format(new java.util.Date(last * 1000));
                    tvTime.setText("First: " + firstStr + "  |  Last: " + lastStr);
                } else {
                    tvTime.setText("Unknown time");
                }
                tvTime.setTextSize(10);
                tvTime.setTextColor(Color.parseColor("#6B7480"));
                tvTime.setPadding(0, 4, 0, 8);
                card.addView(tvTime);

                final boolean finalIsBlocked = isBlocked;
                final Button btnAction = new Button(this);
                btnAction.setText(isBlocked ? "Unblock device" : "Block device");
                btnAction.setTextSize(11);
                btnAction.setTypeface(null, android.graphics.Typeface.BOLD);
                btnAction.setTextColor(isBlocked ? Color.parseColor("#E8EAED") : Color.parseColor("#0B0F17"));
                GradientDrawable btnBg = new GradientDrawable();
                btnBg.setCornerRadius(10 * density);
                btnBg.setColor(isBlocked ? Color.parseColor("#2A3441") : Color.parseColor("#F38020"));
                btnAction.setBackground(btnBg);
                btnAction.setPadding(12, 8, 12, 8);
                btnAction.setAllCaps(false);
                
                btnAction.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        btnAction.setEnabled(false);
                        btnAction.setText("Processing...");
                        if (finalIsBlocked) {
                            unblockDevice(keyName, deviceId, new Runnable() {
                                @Override public void run() {
                                    fetchStatsForAdmin(keyName);
                                }
                            });
                        } else {
                            blockDevice(keyName, deviceId, new Runnable() {
                                @Override public void run() {
                                    fetchStatsForAdmin(keyName);
                                }
                            });
                        }
                    }
                });
                card.addView(btnAction);
                deviceContainer.addView(card);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    scrollView.addView(deviceContainer);
    root.addView(scrollView);

    Button btnClose = new Button(this);
    btnClose.setText("Tutup");
    btnClose.setTextColor(Color.parseColor("#F38020"));
    btnClose.setBackgroundResource(getFnmods("bg_button_outline", "drawable"));
    btnClose.setAllCaps(false);
    root.addView(btnClose);

    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setView(root);
    final AlertDialog dialog = builder.create();
    
    btnClose.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            dialog.dismiss();
        }
    });
    
    dialog.setCancelable(true);
    dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
        @Override
        public void onCancel(DialogInterface d) {
            dialog.dismiss();
        }
    });
    
    dialog.show();
}

// Fungsi baru untuk ambil daftar blokir
private void fetchBlockedDevices() {
    final String workerUrl = mode.getString("cf_worker_url", "");
    if (workerUrl.isEmpty()) {
        UiHelper.showMessage(this, "Worker URL belum diatur.");
        return;
    }

    progressDialog = new ProgressDialog(this);
    progressDialog.setMessage("Mengambil daftar blokir...");
    progressDialog.show();

    new Thread(new Runnable() {
        @Override
        public void run() {
            HttpURLConnection conn = null;
            try {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (progressDialog != null) progressDialog.dismiss();
                        UiHelper.showMessage(LicenseActivity.this, "Fitur daftar blokir akan segera hadir. Untuk sekarang, blokir/unblock dari stats per key.");
                    }
                });
            } catch (final Exception ex) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (progressDialog != null) progressDialog.dismiss();
                        UiHelper.showError(LicenseActivity.this, "Error: " + ex.getMessage());
                    }
                });
            } finally {
                if (conn != null) conn.disconnect();
            }
        }
    }).start();
}

	private void blockDevice(final String keyName, final String deviceId, final Runnable onSuccess) {
    callBlockEndpoint(keyName, deviceId, true, onSuccess);
}

private void unblockDevice(final String keyName, final String deviceId, final Runnable onSuccess) {
    callBlockEndpoint(keyName, deviceId, false, onSuccess);
}

// Ubah callBlockEndpoint untuk menerima callback
private void callBlockEndpoint(final String keyName, final String deviceId, final boolean block, final Runnable onSuccess) {
    final String workerUrl = mode.getString("cf_worker_url", "");
    final String adminSecret = mode.getString("cf_admin_secret", "admin123");
    if (workerUrl.isEmpty()) {
        UiHelper.showMessage(this, "Worker URL belum diatur.");
        if (onSuccess != null) onSuccess.run();
        return;
    }

    progressDialog = new ProgressDialog(this);
    progressDialog.setMessage(block ? "Blocking device..." : "Unblocking device...");
    progressDialog.setCancelable(false);
    progressDialog.show();

    new Thread(new Runnable() {
        @Override
        public void run() {
            HttpURLConnection conn = null;
            BufferedReader reader = null;
            try {
                String endpoint = block ? "/admin/block" : "/admin/unblock";
                URL url = new URL(workerUrl + endpoint);
                conn = (HttpURLConnection) url.openConnection();
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + adminSecret);
                conn.setRequestProperty("Content-Type", "application/json");

                JSONObject body = new JSONObject();
                body.put("device", deviceId);
                body.put("key", keyName);
                if (block) body.put("reason", "Blocked via APK");

                OutputStream out = conn.getOutputStream();
                out.write(body.toString().getBytes("UTF-8"));
                out.flush();
                out.close();

                final int code = conn.getResponseCode();
                final String responseData;
                try (BufferedReader br = new BufferedReader(new InputStreamReader(
                        (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream(), "UTF-8"))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    responseData = sb.toString();
                }

                final boolean success = (code >= 200 && code < 300);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (progressDialog != null) progressDialog.dismiss();
                        if (success) {
                            UiHelper.showMessage(LicenseActivity.this, block
                                    ? ("✅ Device " + deviceId + " DIBLOKIR!")
                                    : ("✅ Device " + deviceId + " DIBUKA BLOKIRNYA!"));
                            // 🔥 Jalankan callback untuk refresh
                            if (onSuccess != null) onSuccess.run();
                        } else {
                            UiHelper.showError(LicenseActivity.this, "Gagal: " + responseData);
                            if (onSuccess != null) onSuccess.run(); // tetap refresh walau gagal? Lebih baik tidak, tapi agar UI tidak hang.
                        }
                    }
                });

            } catch (final Exception e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (progressDialog != null) progressDialog.dismiss();
                        UiHelper.showError(LicenseActivity.this, "Error: " + e.getMessage());
                        if (onSuccess != null) onSuccess.run();
                    }
                });
            } finally {
                if (conn != null) conn.disconnect();
            }
        }
    }).start();
}

	private void showGenerateKeyDialog() {
		LinearLayout layout = new LinearLayout(this);
		layout.setOrientation(LinearLayout.VERTICAL);
		layout.setPadding(36, 16, 36, 16);

		final EditText etName = new EditText(this);
		etName.setHint("LICENSE_KEY (e.g., USER-A42)");
		etName.setSingleLine(true);
		
		final EditText etLimit = new EditText(this);
		etLimit.setHint("Max Registered Devices (e.g., 5)");
		etLimit.setSingleLine(true);
		etLimit.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
		etLimit.setText("5");

		final EditText etDays = new EditText(this);
		etDays.setHint("Active Time in Days (e.g., 30)");
		etDays.setSingleLine(true);
		etDays.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
		etDays.setText("30");

		layout.addView(etName);
		layout.addView(etLimit);
		layout.addView(etDays);

		new AlertDialog.Builder(this)
			.setTitle("Generate License Key")
			.setView(layout)
			.setPositiveButton("Create key", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String key = etName.getText().toString().trim().toUpperCase();
					String limitStr = etLimit.getText().toString().trim();
					String daysStr = etDays.getText().toString().trim();
					
					if (key.isEmpty() || limitStr.isEmpty() || daysStr.isEmpty()) {
						UiHelper.showMessage(LicenseActivity.this, "All inputs are mandatory!");
						return;
					}

					try {
						int limit = Integer.parseInt(limitStr);
						int days = Integer.parseInt(daysStr);
						if (limit <= 0 || days <= 0) {
							UiHelper.showMessage(LicenseActivity.this, "Values must be greater than 0!");
							return;
						}
						long ttlSeconds = days * 86400L;
						writeNewLicenseToKV(key, limit, ttlSeconds);
					} catch (Exception ignored) {
						UiHelper.showMessage(LicenseActivity.this, "Invalid number format!");
					}
				}
			})
			.setNegativeButton("Cancel", null)
			.show();
	}

	private void writeNewLicenseToKV(final String keyName, final int maxDevices, final long ttlSeconds) {
		final String token = mode.getString("cf_api_token", "");
		final String accountId = mode.getString("cf_account_id", "");
		final String kvId = mode.getString("cf_kv_namespace_id", "");

		progressDialog = new ProgressDialog(this);
		progressDialog.setMessage("Saving license key to edge database...");
		progressDialog.setCancelable(false);
		progressDialog.show();

		new Thread(new Runnable() {
			@Override
			public void run() {
				HttpURLConnection conn = null;
				BufferedReader reader = null;
				try {
					URL url = new URL("https://api.cloudflare.com/client/v4/accounts/" + accountId + "/storage/kv/namespaces/" + kvId + "/values/" + keyName + "?expiration_ttl=" + ttlSeconds);
					conn = (HttpURLConnection) url.openConnection();
					conn.setDoOutput(true);
					conn.setRequestMethod("PUT");
					conn.setRequestProperty("Authorization", "Bearer " + token);
					conn.setRequestProperty("Content-Type", "application/json");

					JSONObject payload = new JSONObject();
					payload.put("max_devices", maxDevices);
					payload.put("registered_devices", new JSONArray());

					OutputStream out = conn.getOutputStream();
					out.write(payload.toString().getBytes("UTF-8"));
					out.flush();
					out.close();

					final int code = conn.getResponseCode();
					InputStream in = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
					reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
					StringBuilder sb = new StringBuilder();
					String line;
					while ((line = reader.readLine()) != null) sb.append(line);
					final String responseData = sb.toString();

					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (progressDialog != null) progressDialog.dismiss();
							if (code >= 200 && code < 300) {
								UiHelper.showMessage(LicenseActivity.this, "License Key created successfully! 🛡️");
								syncKeysAndLogs();
							} else {
								UiHelper.showError(LicenseActivity.this, "Failed setup (" + code + "): " + responseData);
							}
						}
					});

				} catch (Exception e) {
					final Exception finalE = e;
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (progressDialog != null) progressDialog.dismiss();
							UiHelper.showError(LicenseActivity.this, "Upload failed: " + finalE.getMessage());
						}
					});
				} finally {
					if (reader != null) {
						try { reader.close(); } catch (Exception ignored) {}
					}
					if (conn != null) conn.disconnect();
				}
			}
		}).start();
	}
	
	// === SCOPE CHECKER (Untuk LicenseActivity) ===
private void handleScopeError(String responseData) {
    try {
        JSONObject obj = new JSONObject(responseData);
        JSONArray errors = obj.getJSONArray("errors");
        if (errors.length() > 0) {
            String msg = errors.getJSONObject(0).getString("message");
            if (msg.toLowerCase().contains("permission") || msg.toLowerCase().contains("scope") || msg.toLowerCase().contains("authorization")) {
                showScopeRequiredDialog(msg);
                return;
            }
        }
        UiHelper.showError(this, "Gagal: " + responseData);
    } catch (Exception e) {
        UiHelper.showError(this, "Error: " + responseData);
    }
}

private void showScopeRequiredDialog(String errorMsg) {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle("Izin Token Kurang");
    builder.setMessage("Token Cloudflare Anda tidak memiliki izin yang cukup.\n\n" +
            "Error: " + errorMsg + "\n\n" +
            "Scope yang wajib diaktifkan:\n" +
            "- Account.Workers Scripts:Edit\n" +
            "- Account.Workers KV:Edit\n" +
            "- Account.Workers Secrets:Edit\n\n" +
            "Klik tombol di bawah untuk buat token baru.");
    builder.setPositiveButton("Buka Browser", new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://dash.cloudflare.com/profile/api-tokens"));
            startActivity(browserIntent);
        }
    });
    builder.setNegativeButton("Tutup", null);
    builder.show();
}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (progressDialog != null && progressDialog.isShowing()) {
			progressDialog.dismiss();
			progressDialog = null;
		}
	}

	public int getFnmods(String name, String type) {
		return this.getBaseContext().getResources().getIdentifier(name, type, this.getBaseContext().getPackageName());
	}
}