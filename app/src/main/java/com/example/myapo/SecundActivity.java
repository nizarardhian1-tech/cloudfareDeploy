package com.example.myapo;

import java.net.URL;
import java.net.HttpURLConnection;
import java.io.InputStream;
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
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;

import com.example.myapo.utils.UiHelper;

@SuppressWarnings("deprecation")
public class SecundActivity extends Activity {
	
	private LinearLayout linearEditorBg;
	private TextView tvWorkerName;
	private TextView tvCopyUrl;
	private EditText etCodeEditor;
	private Button btnSecrets;
	private Button btnDeploy;
	private SharedPreferences mode;
	private String pickedWorkerName;
	private ProgressDialog progressDialog;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_secund);
		initialize();
		initializeLogic();
	}

	private void initialize() {
		linearEditorBg = findViewById(getFnmods("linear_editor_bg","id"));
		tvWorkerName = findViewById(getFnmods("tv_worker_name","id"));
		tvCopyUrl = findViewById(getFnmods("tv_copy_url","id"));
		etCodeEditor = findViewById(getFnmods("et_code_editor","id"));
		btnSecrets = findViewById(getFnmods("btn_secrets","id"));
		btnDeploy = findViewById(getFnmods("btn_deploy","id"));
		mode = getSharedPreferences("mode", Activity.MODE_PRIVATE);
		
		pickedWorkerName = getIntent().getStringExtra("WORKER_NAME");
		if (pickedWorkerName != null) {
			tvWorkerName.setText("Editing: " + pickedWorkerName);
		}
		
		ActionBar actionBar = getActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
			actionBar.setTitle("Script Editor");
		}
		
		btnDeploy.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				deployWorkerCode();
			}
		});

		tvCopyUrl.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				copyLiveUrlToClipboard();
			}
		});

		btnSecrets.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showSecretsManagerDialog();
			}
		});
	}
	
	private void initializeLogic() {
		boolean isNight = mode.getString("night", "").equals("true");
		ActionBar actionBar = getActionBar();
		
		if (isNight) {
			linearEditorBg.setBackgroundColor(Color.parseColor("#121212"));
			tvWorkerName.setTextColor(Color.parseColor("#FFFFFF"));
			tvCopyUrl.setTextColor(Color.parseColor("#F38020"));
			etCodeEditor.setTextColor(Color.parseColor("#E0E0E0"));
			etCodeEditor.setHintTextColor(Color.parseColor("#5F6368"));
			
			GradientDrawable gd = new GradientDrawable();
			gd.setColor(Color.parseColor("#1E1E1E"));
			gd.setCornerRadius(16);
			gd.setStroke(2, Color.parseColor("#333333"));
			etCodeEditor.setBackground(gd);
			
			if (actionBar != null) {
				actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#1E1E1E")));
			}
		} else {
			linearEditorBg.setBackgroundColor(Color.parseColor("#F8F9FA"));
			tvWorkerName.setTextColor(Color.parseColor("#1D1D1D"));
			tvCopyUrl.setTextColor(Color.parseColor("#F38020"));
			etCodeEditor.setTextColor(Color.parseColor("#333333"));
			etCodeEditor.setHintTextColor(Color.parseColor("#A0A0A0"));
			
			GradientDrawable gd = new GradientDrawable();
			gd.setColor(Color.parseColor("#FFFFFF"));
			gd.setCornerRadius(16);
			gd.setStroke(2, Color.parseColor("#DCDCDC"));
			etCodeEditor.setBackground(gd);
			
			if (actionBar != null) {
				actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#F38020")));
			}
		}

		fetchWorkerCode();
	}

	private void copyLiveUrlToClipboard() {
		String subdomain = mode.getString("cf_subdomain", "");
		if (subdomain.isEmpty() || pickedWorkerName == null) {
			UiHelper.showMessage(this, "Subdomain details not synchronized yet.");
			return;
		}

		String liveUrl = "https://" + pickedWorkerName + "." + subdomain + ".workers.dev";
		
		android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE);
		android.content.ClipData clip = android.content.ClipData.newPlainText("Worker Link", liveUrl);
		clipboard.setPrimaryClip(clip);
		
		UiHelper.showMessage(this, "Link Copied: " + liveUrl);
	}

	private void showSecretsManagerDialog() {
		final String token = mode.getString("cf_api_token", "");
		final String accountId = mode.getString("cf_account_id", "");
		
		if (token.isEmpty() || accountId.isEmpty() || pickedWorkerName == null) {
			UiHelper.showMessage(this, "API configuration missing.");
			return;
		}

		progressDialog = new ProgressDialog(this);
		progressDialog.setMessage("Loading secrets...");
		progressDialog.setCancelable(false);
		progressDialog.show();

		new Thread(new Runnable() {
			@Override
			public void run() {
				HttpURLConnection conn = null;
				BufferedReader r = null;
				try {
					URL url = new URL("https://api.cloudflare.com/client/v4/accounts/" + accountId + "/workers/scripts/" + pickedWorkerName + "/secrets");
					conn = (HttpURLConnection) url.openConnection();
					conn.setRequestMethod("GET");
					conn.setRequestProperty("Authorization", "Bearer " + token);

					final int code = conn.getResponseCode();
					InputStream in = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
					r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
					StringBuilder sb = new StringBuilder();
					String line;
					while ((line = r.readLine()) != null) {
						sb.append(line);
					}
					final String jsonResponse = sb.toString();

					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (progressDialog != null) progressDialog.dismiss();
							if (code >= 200 && code < 300) {
								try {
									JSONObject obj = new JSONObject(jsonResponse);
									JSONArray arr = obj.getJSONArray("result");
									displaySecretsList(arr);
								} catch (Exception e) {
									UiHelper.showMessage(SecundActivity.this, "Failed to parse secrets.");
								}
							} else {
								UiHelper.showError(SecundActivity.this, "Failed (" + code + "): " + jsonResponse);
							}
						}
					});

				} catch (Exception e) {
					final Exception finalE = e;
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (progressDialog != null) progressDialog.dismiss();
							UiHelper.showError(SecundActivity.this, "Connection failed: " + finalE.getMessage());
						}
					});
				} finally {
					if (r != null) {
						try { r.close(); } catch (Exception ignored) {}
					}
					if (conn != null) conn.disconnect();
				}
			}
		}).start();
	}

	private void displaySecretsList(final JSONArray arr) throws Exception {
		final String[] secretNames = new String[arr.length()];
		for (int i = 0; i < arr.length(); i++) {
			secretNames[i] = arr.getJSONObject(i).getString("name");
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Secrets: " + pickedWorkerName);
		
		if (secretNames.length == 0) {
			builder.setMessage("No private secrets uploaded yet. Tap 'Add Secret' below to upload your first key.");
		} else {
			builder.setItems(secretNames, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					final String selectedSecret = secretNames[which];
					new AlertDialog.Builder(SecundActivity.this)
						.setTitle("Delete Secret")
						.setMessage("Delete '" + selectedSecret + "' from secure edge storage?")
						.setPositiveButton("Yes, Delete", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface d, int w) {
								deleteSecret(selectedSecret);
							}
						})
						.setNegativeButton("No", null)
						.show();
				}
			});
		}

		builder.setPositiveButton("Add Secret 🔑", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				showAddSecretDialog();
			}
		});
		builder.setNegativeButton("Close", null);
		builder.show();
	}

	private void showAddSecretDialog() {
		LinearLayout layout = new LinearLayout(this);
		layout.setOrientation(LinearLayout.VERTICAL);
		layout.setPadding(36, 16, 36, 16);

		final EditText etName = new EditText(this);
		etName.setHint("NAME (e.g., API_TOKEN_APK)");
		etName.setSingleLine(true);
		
		final EditText etVal = new EditText(this);
		etVal.setHint("Variable secret text value...");
		etVal.setSingleLine(true);

		layout.addView(etName);
		layout.addView(etVal);

		new AlertDialog.Builder(this)
			.setTitle("Upload Encrypted Secret")
			.setView(layout)
			.setPositiveButton("Upload Key 🛡️", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String name = etName.getText().toString().trim().toUpperCase();
					String val = etVal.getText().toString().trim();
					if (!name.isEmpty() && !val.isEmpty()) {
						uploadSecret(name, val);
					} else {
						UiHelper.showMessage(SecundActivity.this, "Fields cannot be empty");
					}
				}
			})
			.setNegativeButton("Cancel", null)
			.show();
	}

	private void uploadSecret(final String name, final String value) {
		final String token = mode.getString("cf_api_token", "");
		final String accountId = mode.getString("cf_account_id", "");

		progressDialog = new ProgressDialog(this);
		progressDialog.setMessage("Encrypting & uploading secret...");
		progressDialog.setCancelable(false);
		progressDialog.show();

		new Thread(new Runnable() {
			@Override
			public void run() {
				HttpURLConnection conn = null;
				BufferedReader r = null;
				try {
					URL url = new URL("https://api.cloudflare.com/client/v4/accounts/" + accountId + "/workers/scripts/" + pickedWorkerName + "/secrets");
					conn = (HttpURLConnection) url.openConnection();
					conn.setDoOutput(true);
					conn.setRequestMethod("PUT");
					conn.setRequestProperty("Authorization", "Bearer " + token);
					conn.setRequestProperty("Content-Type", "application/json");

					JSONObject body = new JSONObject();
					body.put("name", name);
					body.put("text", value);
					body.put("type", "secret_text");

					OutputStream out = conn.getOutputStream();
					out.write(body.toString().getBytes("UTF-8"));
					out.flush();
					out.close();

					final int code = conn.getResponseCode();
					InputStream in = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
					r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
					StringBuilder sb = new StringBuilder();
					String line;
					while ((line = r.readLine()) != null) {
						sb.append(line);
					}
					final String responseData = sb.toString();

					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (progressDialog != null) progressDialog.dismiss();
							if (code >= 200 && code < 300) {
								UiHelper.showMessage(SecundActivity.this, "Secret '" + name + "' uploaded!");
							} else {
								UiHelper.showError(SecundActivity.this, "Failed (" + code + "): " + responseData);
							}
						}
					});

				} catch (Exception e) {
					final Exception finalE = e;
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (progressDialog != null) progressDialog.dismiss();
							UiHelper.showError(SecundActivity.this, "Upload error: " + finalE.getMessage());
						}
					});
				} finally {
					if (r != null) {
						try { r.close(); } catch (Exception ignored) {}
					}
					if (conn != null) conn.disconnect();
				}
			}
		}).start();
	}

	private void deleteSecret(final String name) {
		final String token = mode.getString("cf_api_token", "");
		final String accountId = mode.getString("cf_account_id", "");

		progressDialog = new ProgressDialog(this);
		progressDialog.setMessage("Deleting secret...");
		progressDialog.setCancelable(false);
		progressDialog.show();

		new Thread(new Runnable() {
			@Override
			public void run() {
				HttpURLConnection conn = null;
				BufferedReader r = null;
				try {
					URL url = new URL("https://api.cloudflare.com/client/v4/accounts/" + accountId + "/workers/scripts/" + pickedWorkerName + "/secrets/" + name);
					conn = (HttpURLConnection) url.openConnection();
					conn.setRequestMethod("DELETE");
					conn.setRequestProperty("Authorization", "Bearer " + token);

					final int code = conn.getResponseCode();
					InputStream in = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
					r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
					StringBuilder sb = new StringBuilder();
					String line;
					while ((line = r.readLine()) != null) {
						sb.append(line);
					}
					final String responseData = sb.toString();

					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (progressDialog != null) progressDialog.dismiss();
							if (code >= 200 && code < 300) {
								UiHelper.showMessage(SecundActivity.this, "Secret '" + name + "' deleted.");
							} else {
								UiHelper.showError(SecundActivity.this, "Delete failed: " + responseData);
							}
						}
					});

				} catch (Exception e) {
					final Exception finalE = e;
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (progressDialog != null) progressDialog.dismiss();
							UiHelper.showError(SecundActivity.this, "Delete Error: " + finalE.getMessage());
						}
					});
				} finally {
					if (r != null) {
						try { r.close(); } catch (Exception ignored) {}
					}
					if (conn != null) conn.disconnect();
				}
			}
		}).start();
	}

	private void fetchWorkerCode() {
		final String token = mode.getString("cf_api_token", "");
		final String accountId = mode.getString("cf_account_id", "");

		if (token.isEmpty() || accountId.isEmpty() || pickedWorkerName == null) {
			UiHelper.showMessage(this, "Configuration details missing.");
			return;
		}

		progressDialog = new ProgressDialog(this);
		progressDialog.setMessage("Downloading Worker script from Cloudflare...");
		progressDialog.setCancelable(false);
		progressDialog.show();

		new Thread(new Runnable() {
			@Override
			public void run() {
				HttpURLConnection conn = null;
				BufferedReader reader = null;
				try {
					URL url = new URL("https://api.cloudflare.com/client/v4/accounts/" + accountId + "/workers/scripts/" + pickedWorkerName);
					conn = (HttpURLConnection) url.openConnection();
					conn.setRequestMethod("GET");
					conn.setRequestProperty("Authorization", "Bearer " + token);
					
					final int responseCode = conn.getResponseCode();
					InputStream in = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();

					reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
					final StringBuilder sb = new StringBuilder();
					String line;
					while ((line = reader.readLine()) != null) {
						sb.append(line).append("\n");
					}
					
					final String responseData = sb.toString();

					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (progressDialog != null) progressDialog.dismiss();
							if (responseCode >= 200 && responseCode < 300) {
								etCodeEditor.setText(responseData.trim());
								UiHelper.showMessage(SecundActivity.this, "Script loaded!");
							} else {
								UiHelper.showError(SecundActivity.this, "Could not retrieve script (" + responseCode + "):\n" + responseData);
							}
						}
					});

				} catch (Exception e) {
					final Exception finalE = e;
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (progressDialog != null) progressDialog.dismiss();
							UiHelper.showError(SecundActivity.this, "Download Error: " + finalE.getMessage());
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

	private void deployWorkerCode() {
		final String token = mode.getString("cf_api_token", "");
		final String accountId = mode.getString("cf_account_id", "");
		final String code = etCodeEditor.getText().toString();

		if (token.isEmpty() || accountId.isEmpty() || pickedWorkerName == null) {
			UiHelper.showMessage(this, "API config details missing.");
			return;
		}
		
		if (code.trim().isEmpty()) {
			UiHelper.showMessage(this, "Code cannot be empty!");
			return;
		}

		progressDialog = new ProgressDialog(this);
		progressDialog.setMessage("Uploading script to Cloudflare edge...");
		progressDialog.setCancelable(false);
		progressDialog.show();

		new Thread(new Runnable() {
			@Override
			public void run() {
				HttpURLConnection conn = null;
				BufferedReader reader = null;
				try {
					URL url = new URL("https://api.cloudflare.com/client/v4/accounts/" + accountId + "/workers/scripts/" + pickedWorkerName);
					conn = (HttpURLConnection) url.openConnection();
					conn.setDoOutput(true);
					conn.setRequestMethod("PUT");
					conn.setRequestProperty("Authorization", "Bearer " + token);
					conn.setRequestProperty("Content-Type", "application/javascript");

					OutputStream out = conn.getOutputStream();
					out.write(code.getBytes("UTF-8"));
					out.flush();
					out.close();

					final int responseCode = conn.getResponseCode();
					InputStream in = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();

					reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
					final StringBuilder sb = new StringBuilder();
					String line;
					while ((line = reader.readLine()) != null) {
						sb.append(line);
					}

					final String responseData = sb.toString();

					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (progressDialog != null) progressDialog.dismiss();
							if (responseCode >= 200 && responseCode < 300) {
								try {
									JSONObject obj = new JSONObject(responseData);
									if (obj.optBoolean("success", false)) {
										UiHelper.showMessage(SecundActivity.this, "Script uploaded and deployed! 🚀");
									} else {
										UiHelper.showMessage(SecundActivity.this, "Deployed with warning:\n" + responseData);
									}
								} catch (Exception e) {
									UiHelper.showMessage(SecundActivity.this, "Deployment complete! Output: " + responseData);
								}
							} else {
								UiHelper.showError(SecundActivity.this, "Edge deployment failed (" + responseCode + "):\n" + responseData);
							}
						}
					});

				} catch (Exception e) {
					final Exception finalE = e;
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (progressDialog != null) progressDialog.dismiss();
							UiHelper.showError(SecundActivity.this, "Network Error during deployment: " + finalE.getMessage());
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

	@Override
	public boolean onOptionsItemSelected(android.view.MenuItem item) {
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