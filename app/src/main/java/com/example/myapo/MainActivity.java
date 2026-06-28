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
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.myapo.utils.UiHelper;

@SuppressWarnings("deprecation")
public class MainActivity extends Activity {
	
	private LinearLayout linearMainBg;
	private LinearLayout cardWorkers;
	private LinearLayout cardMonitor;
	private LinearLayout cardDatabase;
	private LinearLayout linearWorkersList;
	
	private TextView tvAccount, tvSubdomain;
	private TextView tvCardTitle1, tvCardTitleDb, tvCardDescDb, tvMonitorTitle;
	private TextView tvStatStatus, tvStatLimit, tvStatCount;
	private View viewStatusDot;
	private TextView tvListPlaceholder, tvRefreshBtn, tvCreateWorkerBtn, tvSwitchAccount;
	private Button btnManageLicenses;
	
	private EditText etSearchWorker;
	
	private SharedPreferences mode;
	private ProgressDialog progressDialog;
	
	private JSONArray cachedWorkersArray = new JSONArray();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initialize();
		initializeLogic();
	}

	private void initialize() {
		linearMainBg = findViewById(getFnmods("linear_main_bg","id"));
		cardWorkers = findViewById(getFnmods("card_workers","id"));
		cardMonitor = findViewById(getFnmods("card_monitor","id"));
		cardDatabase = findViewById(getFnmods("card_database","id"));
		linearWorkersList = findViewById(getFnmods("linear_workers_list","id"));
		
		tvAccount = findViewById(getFnmods("tv_account","id"));
		tvSubdomain = findViewById(getFnmods("tv_subdomain","id"));
		tvCardTitle1 = findViewById(getFnmods("tv_card_title1","id"));
		tvCardTitleDb = findViewById(getFnmods("tv_card_title_db","id"));
		tvCardDescDb = findViewById(getFnmods("tv_card_desc_db","id"));
		tvMonitorTitle = findViewById(getFnmods("tv_monitor_title","id"));
		tvStatStatus = findViewById(getFnmods("tv_stat_status","id"));
		viewStatusDot = findViewById(getFnmods("view_status_dot","id"));
		tvStatLimit = findViewById(getFnmods("tv_stat_limit","id"));
		tvStatCount = findViewById(getFnmods("tv_stat_count","id"));
		tvListPlaceholder = findViewById(getFnmods("tv_list_placeholder","id"));
		tvRefreshBtn = findViewById(getFnmods("tv_refresh_btn","id"));
		tvCreateWorkerBtn = findViewById(getFnmods("tv_create_worker_btn","id"));
		tvSwitchAccount = findViewById(getFnmods("tv_switch_account","id"));
		btnManageLicenses = findViewById(getFnmods("btn_manage_licenses","id"));
		
		etSearchWorker = findViewById(getFnmods("et_search_worker","id"));
		
		mode = getSharedPreferences("mode", Activity.MODE_PRIVATE);

		// SEARCH
		etSearchWorker.addTextChangedListener(new android.text.TextWatcher() {
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				filterWorkers(s.toString());
			}
			public void afterTextChanged(android.text.Editable s) {}
		});

		// SWITCH ACCOUNT
		tvSwitchAccount.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showSwitchAccountDialog();
			}
		});

		tvRefreshBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				fetchWorkersList(true);
				fetchAccountSubdomain();
			}
		});

		tvAccount.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				showApiTokenDialog();
			}
		});

		btnManageLicenses.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this, LicenseActivity.class);
				startActivity(intent);
			}
		});

		tvCreateWorkerBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showCreateWorkerDialog();
			}
		});
	}
	
	private void initializeLogic() {
		mViewToolbar.MFnmodsToolbar(MainActivity.this);
		checkTokenStatus();
		applyTheme();
		
		loadCachedWorkers();
		fetchWorkersList(false);
		fetchAccountSubdomain();
	}

	private void checkTokenStatus() {
		String currentToken = mode.getString("cf_api_token", "");
		String accountName = mode.getString("cf_account_name", "Akun Default");
		if (currentToken.isEmpty()) {
			tvAccount.setText("Tap to setup token");
			tvSubdomain.setVisibility(View.GONE);
			tvSwitchAccount.setVisibility(View.GONE);
		} else {
			tvAccount.setText(accountName);
			tvSwitchAccount.setVisibility(View.VISIBLE);
		}
	}

	// === SWITCH ACCOUNT ===
	private void showSwitchAccountDialog() {
		String accountsJson = mode.getString("cf_accounts_list", "[]");
		final String[] accountNames;
		final String[] accountData;
		try {
			JSONArray arr = new JSONArray(accountsJson);
			accountNames = new String[arr.length()];
			accountData = new String[arr.length()];
			for (int i = 0; i < arr.length(); i++) {
				JSONObject obj = arr.getJSONObject(i);
				accountNames[i] = obj.getString("name") + " (" + obj.getString("account_id").substring(0, 8) + "...)";
				accountData[i] = obj.toString();
			}
		} catch (Exception e) {
			UiHelper.showMessage(this, "Gagal memuat daftar akun.");
			return;
		}

		if (accountNames.length == 0) {
			UiHelper.showMessage(this, "Belum ada akun tersimpan. Setup token dulu.");
			return;
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Pilih Akun Cloudflare");
		builder.setItems(accountNames, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				try {
					JSONObject selected = new JSONObject(accountData[which]);
					String token = selected.getString("token");
					String accountId = selected.getString("account_id");
					String name = selected.getString("name");
					
					mode.edit().putString("cf_api_token", token).commit();
					mode.edit().putString("cf_account_id", accountId).commit();
					mode.edit().putString("cf_account_name", name).commit();
					
					UiHelper.showMessage(MainActivity.this, "Berhasil switch ke: " + name);
					checkTokenStatus();
					fetchWorkersList(true);
					fetchAccountSubdomain();
				} catch (Exception e) {
					UiHelper.showError(MainActivity.this, "Gagal switch akun.");
				}
			}
		});
		builder.setNegativeButton("Tambah Akun +", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				showApiTokenDialog();
			}
		});
		builder.setNeutralButton("Hapus Akun", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				showDeleteAccountDialog();
			}
		});
		builder.show();
	}

	private void showDeleteAccountDialog() {
		String accountsJson = mode.getString("cf_accounts_list", "[]");
		final String[] accountNames;
		final JSONArray arr;
		try {
			arr = new JSONArray(accountsJson);
			accountNames = new String[arr.length()];
			for (int i = 0; i < arr.length(); i++) {
				accountNames[i] = arr.getJSONObject(i).getString("name");
			}
		} catch (Exception e) {
			UiHelper.showMessage(this, "Error membaca daftar.");
			return;
		}
		if (accountNames.length == 0) return;

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Hapus Akun");
		builder.setItems(accountNames, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				try {
					arr.remove(which);
					mode.edit().putString("cf_accounts_list", arr.toString()).commit();
					UiHelper.showMessage(MainActivity.this, "Akun dihapus.");
					checkTokenStatus();
				} catch (Exception e) {
					UiHelper.showError(MainActivity.this, "Gagal hapus.");
				}
			}
		});
		builder.show();
	}

	// === SETUP TOKEN ===
	private void showApiTokenDialog() {
		final EditText inputToken = new EditText(this);
		inputToken.setHint("Paste Cloudflare API Token...");
		inputToken.setSingleLine(true);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Tambah Akun Cloudflare");
		builder.setMessage("Masukkan API Token. Aplikasi akan otomatis mendeteksi Account ID.");
		builder.setView(inputToken);
		builder.setPositiveButton("Simpan & Verifikasi", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String newToken = inputToken.getText().toString().trim();
				if (newToken.isEmpty()) {
					UiHelper.showMessage(MainActivity.this, "Token tidak boleh kosong!");
					return;
				}
				mode.edit().putString("cf_api_token", newToken).commit();
				checkTokenStatus(); 
				testAndFetchAccountId(true);
			}
		});
		builder.setNegativeButton("Batal", null);
		builder.show();
	}

	// === VERIFIKASI TOKEN ===
	private void testAndFetchAccountId(final boolean saveToAccountList) {
		final String token = mode.getString("cf_api_token", "");
		if (token.isEmpty()) {
			UiHelper.showMessage(this, "Token kosong!");
			return;
		}

		progressDialog = new ProgressDialog(this);
		progressDialog.setMessage("Verifikasi Token...");
		progressDialog.setCancelable(false);
		progressDialog.show();

		new Thread(new Runnable() {
			@Override
			public void run() {
				HttpURLConnection conn = null;
				BufferedReader reader = null;
				try {
					URL url = new URL("https://api.cloudflare.com/client/v4/accounts");
					conn = (HttpURLConnection) url.openConnection();
					conn.setRequestMethod("GET");
					conn.setRequestProperty("Authorization", "Bearer " + token);
					conn.setRequestProperty("Content-Type", "application/json");
					
					int responseCode = conn.getResponseCode();
					InputStream in = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
					reader = new BufferedReader(new InputStreamReader(in));
					StringBuilder sb = new StringBuilder();
					String line;
					while ((line = reader.readLine()) != null) sb.append(line);
					final String jsonResponse = sb.toString();
					
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (progressDialog != null) progressDialog.dismiss();
							try {
								JSONObject jsonObj = new JSONObject(jsonResponse);
								boolean success = jsonObj.getBoolean("success");
								
								if (success) {
									JSONArray resultArr = jsonObj.getJSONArray("result");
									if (resultArr.length() > 0) {
										JSONObject account = resultArr.getJSONObject(0);
										String accountId = account.getString("id");
										String accountName = account.getString("name");
										
										mode.edit().putString("cf_account_id", accountId).commit();
										mode.edit().putString("cf_account_name", accountName).commit();
										
										if (saveToAccountList) {
											saveAccountToPreferences(accountName, accountId, token);
										}
										
										UiHelper.showMessage(MainActivity.this, "✅ Berhasil! Akun: " + accountName);
										fetchWorkersList(false);
										fetchAccountSubdomain();
										checkTokenStatus();
									}
								} else {
									handleScopeError(jsonResponse);
								}
							} catch (Exception ex) {
								UiHelper.showError(MainActivity.this, "Gagal parsing response API.");
							}
						}
					});
					
				} catch (final Exception ex) {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (progressDialog != null) progressDialog.dismiss();
							UiHelper.showError(MainActivity.this, "Network error: " + ex.getMessage());
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

	private void saveAccountToPreferences(String name, String accountId, String token) {
		try {
			String accountsJson = mode.getString("cf_accounts_list", "[]");
			JSONArray arr = new JSONArray(accountsJson);
			
			boolean exists = false;
			for (int i = 0; i < arr.length(); i++) {
				if (arr.getJSONObject(i).getString("account_id").equals(accountId)) {
					exists = true;
					break;
				}
			}
			if (!exists) {
				JSONObject newAccount = new JSONObject();
				newAccount.put("name", name);
				newAccount.put("account_id", accountId);
				newAccount.put("token", token);
				arr.put(newAccount);
				mode.edit().putString("cf_accounts_list", arr.toString()).commit();
			}
		} catch (Exception e) {
			// ignore
		}
	}

	// === SCOPE CHECKER ===
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
			UiHelper.showError(MainActivity.this, "Gagal: " + responseData);
		} catch (Exception e) {
			UiHelper.showError(MainActivity.this, "Error tidak dikenal: " + responseData);
		}
	}

	private void showScopeRequiredDialog(String errorMsg) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Izin Token Kurang");
		builder.setMessage("Token Cloudflare Anda tidak memiliki izin yang cukup.\n\n" +
				"Error: " + errorMsg + "\n\n" +
				"Scope yang wajib diaktifkan (centang):\n" +
				"- Account.Workers Scripts:Edit\n" +
				"- Account.Workers KV:Edit\n" +
				"- Account.Workers Secrets:Edit\n" +
				"- Account.Workers Subdomain:Edit (opsional)\n\n" +
				"Klik tombol di bawah untuk membuat token baru di browser.");
		builder.setPositiveButton("Buka browser (buat token)", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://dash.cloudflare.com/profile/api-tokens"));
				startActivity(browserIntent);
			}
		});
		builder.setNegativeButton("Tutup", null);
		builder.show();
	}

	// === CREATE WORKER ===
	private void showCreateWorkerDialog() {
		final EditText input = new EditText(this);
		input.setHint("script-name (e.g. apk-validator)");
		input.setSingleLine(true);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Create new worker");
		builder.setMessage("Inisialisasi script Worker baru di Cloudflare.");
		builder.setView(input);
		builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String name = input.getText().toString().trim().toLowerCase().replaceAll(" ", "-");
				if (!name.isEmpty()) {
					createWorkerOnCloudflare(name);
				} else {
					UiHelper.showMessage(MainActivity.this, "Nama tidak boleh kosong!");
				}
			}
		});
		builder.setNegativeButton("Cancel", null);
		builder.show();
	}

	private void createWorkerOnCloudflare(final String scriptName) {
		final String token = mode.getString("cf_api_token", "");
		final String accountId = mode.getString("cf_account_id", "");
		
		if (token.isEmpty() || accountId.isEmpty()) {
			UiHelper.showMessage(this, "Token belum diatur.");
			return;
		}

		progressDialog = new ProgressDialog(this);
		progressDialog.setMessage("Deploying script...");
		progressDialog.setCancelable(false);
		progressDialog.show();

		new Thread(new Runnable() {
			@Override
			public void run() {
				HttpURLConnection conn = null;
				BufferedReader reader = null;
				try {
					URL url = new URL("https://api.cloudflare.com/client/v4/accounts/" + accountId + "/workers/scripts/" + scriptName);
					conn = (HttpURLConnection) url.openConnection();
					conn.setDoOutput(true);
					conn.setRequestMethod("PUT");
					conn.setRequestProperty("Authorization", "Bearer " + token);
					conn.setRequestProperty("Content-Type", "application/javascript");

					String starterCode = "addEventListener('fetch', event => {\n" +
										 "  event.respondWith(handleRequest(event.request))\n" +
										 "})\n\n" +
										 "async function handleRequest(request) {\n" +
										 "  return new Response('New script initialized!', { status: 200 })\n" +
										 "}";

					OutputStream out = conn.getOutputStream();
					out.write(starterCode.getBytes("UTF-8"));
					out.flush();
					out.close();

					final int responseCode = conn.getResponseCode();
					InputStream in = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
					reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
					StringBuilder sb = new StringBuilder();
					String line;
					while ((line = reader.readLine()) != null) sb.append(line);
					final String responseData = sb.toString();

					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (progressDialog != null) progressDialog.dismiss();
							if (responseCode >= 200 && responseCode < 300) {
								UiHelper.showMessage(MainActivity.this, "Worker '" + scriptName + "' created!");
								fetchWorkersList(false); 
							} else {
								if (responseData.contains("permission") || responseData.contains("scope")) {
									handleScopeError(responseData);
								} else {
									UiHelper.showError(MainActivity.this, "Gagal buat: " + responseData);
								}
							}
						}
					});

				} catch (final Exception ex) {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (progressDialog != null) progressDialog.dismiss();
							UiHelper.showError(MainActivity.this, "Error: " + ex.getMessage());
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
	
	// === DELETE WORKER ===
	private void confirmDeleteWorker(final String workerName) {
		new AlertDialog.Builder(this)
			.setTitle("Hapus Worker")
			.setMessage("Yakin ingin menghapus script '" + workerName + "'? Tindakan ini permanen!")
			.setPositiveButton("Ya, Hapus", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					deleteWorkerFromCloudflare(workerName);
				}
			})
			.setNegativeButton("Batal", null)
			.show();
	}

	private void deleteWorkerFromCloudflare(final String workerName) {
		final String token = mode.getString("cf_api_token", "");
		final String accountId = mode.getString("cf_account_id", "");
		
		if (token.isEmpty() || accountId.isEmpty()) {
			UiHelper.showMessage(this, "Token belum diatur.");
			return;
		}

		progressDialog = new ProgressDialog(this);
		progressDialog.setMessage("Menghapus " + workerName + "...");
		progressDialog.setCancelable(false);
		progressDialog.show();

		new Thread(new Runnable() {
			@Override
			public void run() {
				HttpURLConnection conn = null;
				BufferedReader reader = null;
				try {
					URL url = new URL("https://api.cloudflare.com/client/v4/accounts/" + accountId + "/workers/scripts/" + workerName);
					conn = (HttpURLConnection) url.openConnection();
					conn.setRequestMethod("DELETE");
					conn.setRequestProperty("Authorization", "Bearer " + token);

					final int responseCode = conn.getResponseCode();
					InputStream in = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
					reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
					StringBuilder sb = new StringBuilder();
					String line;
					while ((line = reader.readLine()) != null) sb.append(line);
					final String responseData = sb.toString();

					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (progressDialog != null) progressDialog.dismiss();
							if (responseCode >= 200 && responseCode < 300) {
								UiHelper.showMessage(MainActivity.this, "🗑️ Worker '" + workerName + "' berhasil dihapus!");
								fetchWorkersList(false);
							} else {
								if (responseData.contains("permission") || responseData.contains("scope")) {
									handleScopeError(responseData);
								} else {
									UiHelper.showError(MainActivity.this, "Gagal hapus: " + responseData);
								}
							}
						}
					});

				} catch (final Exception ex) {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (progressDialog != null) progressDialog.dismiss();
							UiHelper.showError(MainActivity.this, "Error: " + ex.getMessage());
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

	// === THEME ===
	private void applyTheme() {
		boolean isNight = mode.getString("night", "").equals("true");
		ActionBar actionBar = getActionBar();

		// Explicit palette per manual toggle state (independent of system DayNight,
		// since this screen has its own dark/light switch via the ActionBar button)
		int cardBg = isNight ? Color.parseColor("#141B26") : Color.parseColor("#FFFFFF");
		int cardStroke = isNight ? Color.parseColor("#2A3441") : Color.parseColor("#E2E5EA");
		int mainBg = isNight ? Color.parseColor("#0B0F17") : Color.parseColor("#F4F5F7");

		linearMainBg.setBackgroundColor(mainBg);

		GradientDrawable gdWorkers = new GradientDrawable();
		gdWorkers.setColor(cardBg);
		gdWorkers.setCornerRadius(dpToPx(18));
		gdWorkers.setStroke(dpToPx(1), cardStroke);
		cardWorkers.setBackground(gdWorkers);

		GradientDrawable gdMonitor = new GradientDrawable();
		gdMonitor.setColor(cardBg);
		gdMonitor.setCornerRadius(dpToPx(18));
		gdMonitor.setStroke(dpToPx(1), cardStroke);
		cardMonitor.setBackground(gdMonitor);

		GradientDrawable gdDb = new GradientDrawable();
		gdDb.setColor(cardBg);
		gdDb.setCornerRadius(dpToPx(18));
		gdDb.setStroke(dpToPx(1), cardStroke);
		cardDatabase.setBackground(gdDb);

		int primaryColor = isNight ? Color.parseColor("#E8EAED") : Color.parseColor("#161B22");
		int secondaryColor = isNight ? Color.parseColor("#8A94A3") : Color.parseColor("#5B6472");

		tvCardTitle1.setTextColor(primaryColor);
		tvCardTitleDb.setTextColor(primaryColor);
		tvCardDescDb.setTextColor(secondaryColor);
		tvMonitorTitle.setTextColor(primaryColor);

		TextView tvLabel1 = findViewById(getFnmods("tv_label_status", "id"));
		TextView tvLabel2 = findViewById(getFnmods("tv_label_limit", "id"));
		TextView tvLabel3 = findViewById(getFnmods("tv_label_count", "id"));
		if (tvLabel1 != null) tvLabel1.setTextColor(secondaryColor);
		if (tvLabel2 != null) tvLabel2.setTextColor(secondaryColor);
		if (tvLabel3 != null) tvLabel3.setTextColor(secondaryColor);

		tvStatLimit.setTextColor(primaryColor);
		if (!tvStatStatus.getText().toString().equals("Online") && !tvStatStatus.getText().toString().equals("Offline")) {
			tvStatStatus.setTextColor(primaryColor);
		}
		tvStatCount.setTextColor(primaryColor);
		etSearchWorker.setTextColor(primaryColor);

		if (actionBar != null) {
			int headerColor = isNight ? Color.parseColor("#0E1420") : Color.parseColor("#161D29");
			actionBar.setBackgroundDrawable(new ColorDrawable(headerColor));
		}

		loadCachedWorkers();
	}

	private int dpToPx(int dp) {
		float density = getResources().getDisplayMetrics().density;
		return (int) (dp * density);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem menuItem) {
		if (menuItem.getItemId() == android.R.id.home) {
			boolean isNight = mode.getString("night", "").equals("true");
			mode.edit().putString("night", isNight ? "false" : "true").commit();
			applyTheme();
			return true;
		}
		return super.onOptionsItemSelected(menuItem);
	}
	
	private void loadCachedWorkers() {
		String cached = mode.getString("cf_cached_scripts", "{}");
		if (!cached.isEmpty()) {
			try {
				JSONObject jsonObj = new JSONObject(cached);
				if (jsonObj.has("result")) {
					cachedWorkersArray = jsonObj.getJSONArray("result");
					populateWorkersList(cachedWorkersArray);
					tvListPlaceholder.setVisibility(cachedWorkersArray.length() == 0 ? View.VISIBLE : View.GONE);
					tvStatStatus.setText("Cached");
					updateStatusDot(false);
					tvStatStatus.setTextColor((mode.getString("night","").equals("true") ? Color.parseColor("#FFA75C") : Color.parseColor("#D96B16")));
				}
			} catch (Exception ignored) {}
		}
	}

	// === RENDER LIST ===
	private void populateWorkersList(JSONArray resultArr) {
		linearWorkersList.removeAllViews();
		
		if (resultArr == null || resultArr.length() == 0) {
			tvStatCount.setText("0");
			tvListPlaceholder.setVisibility(View.VISIBLE);
			return;
		}
		
		tvStatCount.setText(String.valueOf(resultArr.length()));
		tvListPlaceholder.setVisibility(View.GONE);
		
		boolean isNight = mode.getString("night", "").equals("true");
		int primaryTextColor = isNight ? Color.parseColor("#E8EAED") : Color.parseColor("#161B22");
		int secondaryTextColor = isNight ? Color.parseColor("#8A94A3") : Color.parseColor("#5B6472");
		int dividerColor = isNight ? Color.parseColor("#2A3441") : Color.parseColor("#E2E5EA");

		for (int j = 0; j < resultArr.length(); j++) {
			try {
				JSONObject script = resultArr.getJSONObject(j);
				final String workerName = script.getString("id");
				String modifiedOnStr = script.optString("modified_on", "");
				if (modifiedOnStr.contains("T")) modifiedOnStr = modifiedOnStr.split("T")[0];
				
				LinearLayout row = new LinearLayout(this);
				row.setOrientation(LinearLayout.HORIZONTAL);
				row.setPadding(12, 16, 12, 16);
				row.setGravity(android.view.Gravity.CENTER_VERTICAL);
				row.setClickable(true);
				row.setFocusable(true);
				android.util.TypedValue outValue = new android.util.TypedValue();
				getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
				row.setBackgroundResource(outValue.resourceId);
				
				LinearLayout leftContainer = new LinearLayout(this);
				leftContainer.setOrientation(LinearLayout.VERTICAL);
				LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(0, -2, 1.0f);
				leftContainer.setLayoutParams(leftParams);
				
				LinearLayout headerRow = new LinearLayout(this);
				headerRow.setOrientation(LinearLayout.HORIZONTAL);
				headerRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
				
				TextView tvName = new TextView(this);
				tvName.setText(workerName);
				tvName.setTextSize(15);
				tvName.setTypeface(null, android.graphics.Typeface.BOLD);
				tvName.setTextColor(primaryTextColor);
				headerRow.addView(tvName);
				
				TextView tvBadge = new TextView(this);
				tvBadge.setText("JS");
				tvBadge.setTextSize(10);
				tvBadge.setTypeface(null, android.graphics.Typeface.BOLD);
				tvBadge.setTextColor(Color.parseColor("#FFFFFF"));
				GradientDrawable badgeBg = new GradientDrawable();
				badgeBg.setColor(Color.parseColor("#F38020"));
				badgeBg.setCornerRadius(100 * getResources().getDisplayMetrics().density);
				tvBadge.setBackground(badgeBg);
				tvBadge.setPadding(14, 4, 14, 4);
				headerRow.addView(tvBadge);
				leftContainer.addView(headerRow);
				
				TextView tvSub = new TextView(this);
				tvSub.setText("Updated: " + (modifiedOnStr.isEmpty() ? "N/A" : modifiedOnStr));
				tvSub.setTextSize(11);
				tvSub.setPadding(0, 4, 0, 0);
				tvSub.setTextColor(secondaryTextColor);
				leftContainer.addView(tvSub);
				
				row.addView(leftContainer);
				
				// Tombol Delete
				TextView tvDelete = new TextView(this);
				tvDelete.setText("Hapus");
				tvDelete.setTextSize(12);
				tvDelete.setTypeface(null, android.graphics.Typeface.BOLD);
				tvDelete.setTextColor(Color.parseColor("#D6483F"));
				tvDelete.setPadding(20, 8, 8, 8);
				tvDelete.setClickable(true);
				tvDelete.setFocusable(true);
				tvDelete.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						confirmDeleteWorker(workerName);
					}
				});
				row.addView(tvDelete);
				
				row.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent intent = new Intent(MainActivity.this, SecundActivity.class);
						intent.putExtra("WORKER_NAME", workerName);
						startActivity(intent);
					}
				});
				
				linearWorkersList.addView(row);
				
				if (j < resultArr.length() - 1) {
					View divider = new View(this);
					LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(-1, 2);
					divParams.setMargins(8, 0, 8, 0);
					divider.setLayoutParams(divParams);
					divider.setBackgroundColor(dividerColor);
					linearWorkersList.addView(divider);
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	// === SEARCH ===
	private void filterWorkers(String query) {
		if (cachedWorkersArray == null || cachedWorkersArray.length() == 0) {
			linearWorkersList.removeAllViews();
			tvListPlaceholder.setVisibility(View.VISIBLE);
			return;
		}
		
		if (query.isEmpty()) {
			populateWorkersList(cachedWorkersArray);
			return;
		}

		JSONArray filtered = new JSONArray();
		try {
			for (int i = 0; i < cachedWorkersArray.length(); i++) {
				JSONObject item = cachedWorkersArray.getJSONObject(i);
				String name = item.getString("id");
				if (name.toLowerCase().contains(query.toLowerCase())) {
					filtered.put(item);
				}
			}
			populateWorkersList(filtered);
			if (filtered.length() == 0) {
				tvListPlaceholder.setVisibility(View.VISIBLE);
				tvListPlaceholder.setText("Script '" + query + "' tidak ditemukan.");
			} else {
				tvListPlaceholder.setVisibility(View.GONE);
			}
		} catch (Exception e) {
			// ignore
		}
	}

	// === FETCH WORKERS ===
	private void fetchWorkersList(final boolean showBlocking) {
		final String token = mode.getString("cf_api_token", "");
		final String accountId = mode.getString("cf_account_id", "");
		
		if (token.isEmpty() || accountId.isEmpty()) {
			tvListPlaceholder.setText("Set your Cloudflare API Token above to begin.");
			tvStatStatus.setText("Unconfigured");
			updateStatusDot(false);
			tvStatStatus.setTextColor((mode.getString("night","").equals("true") ? Color.parseColor("#FFA75C") : Color.parseColor("#D96B16")));
			return;
		}

		tvListPlaceholder.setText("Synchronizing...");
		
		if (showBlocking) {
			progressDialog = new ProgressDialog(this);
			progressDialog.setMessage("Synchronizing Scripts...");
			progressDialog.setCancelable(false);
			progressDialog.show();
		}

		new Thread(new Runnable() {
			@Override
			public void run() {
				HttpURLConnection conn = null;
				BufferedReader reader = null;
				try {
					URL url = new URL("https://api.cloudflare.com/client/v4/accounts/" + accountId + "/workers/scripts");
					conn = (HttpURLConnection) url.openConnection();
					conn.setRequestMethod("GET");
						conn.setRequestProperty("Authorization", "Bearer " + token);
					conn.setRequestProperty("Content-Type", "application/json");

					final int responseCode = conn.getResponseCode();
					InputStream in = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
					reader = new BufferedReader(new InputStreamReader(in));
					StringBuilder sb = new StringBuilder();
					String line;
					while ((line = reader.readLine()) != null) sb.append(line);
					final String jsonResponse = sb.toString();

					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (progressDialog != null) progressDialog.dismiss();
							try {
								if (responseCode < 200 || responseCode >= 300) {
									tvListPlaceholder.setText("Error (" + responseCode + ")");
									tvStatStatus.setText("Error");
									updateStatusDot(false);
									tvStatStatus.setTextColor((mode.getString("night","").equals("true") ? Color.parseColor("#FF6B61") : Color.parseColor("#D6483F")));
									if (jsonResponse.contains("permission") || jsonResponse.contains("scope")) {
										handleScopeError(jsonResponse);
									}
									return;
								}

								JSONObject jsonObj = new JSONObject(jsonResponse);
								boolean success = jsonObj.getBoolean("success");

								if (success) {
									JSONArray resultArr = jsonObj.getJSONArray("result");
									cachedWorkersArray = resultArr;
									populateWorkersList(resultArr);
									tvListPlaceholder.setVisibility(resultArr.length() == 0 ? View.VISIBLE : View.GONE);
									tvStatStatus.setText("Online");
									updateStatusDot(true);
									tvStatStatus.setTextColor((mode.getString("night","").equals("true") ? Color.parseColor("#3DDC97") : Color.parseColor("#1E9E6B")));
									mode.edit().putString("cf_cached_scripts", jsonResponse).commit();
								} else {
									if (jsonResponse.contains("permission") || jsonResponse.contains("scope")) {
										handleScopeError(jsonResponse);
									} else {
										tvListPlaceholder.setText("Authorization Rejected");
										tvStatStatus.setText("Rejected");
										updateStatusDot(false);
										tvStatStatus.setTextColor((mode.getString("night","").equals("true") ? Color.parseColor("#FF6B61") : Color.parseColor("#D6483F")));
									}
								}
							} catch (Exception ex) {
								tvListPlaceholder.setText("Failed to parse data");
								tvStatStatus.setText("Parser Error");
								updateStatusDot(false);
								tvStatStatus.setTextColor((mode.getString("night","").equals("true") ? Color.parseColor("#FF6B61") : Color.parseColor("#D6483F")));
							}
						}
					});
				} catch (final Exception ex) {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (progressDialog != null) progressDialog.dismiss();
							tvListPlaceholder.setText("Network error.");
							tvStatStatus.setText("Offline");
							updateStatusDot(false);
							tvStatStatus.setTextColor((mode.getString("night","").equals("true") ? Color.parseColor("#FF6B61") : Color.parseColor("#D6483F")));
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
	
	private void fetchAccountSubdomain() {
		final String token = mode.getString("cf_api_token", "");
		final String accountId = mode.getString("cf_account_id", "");
		if (token.isEmpty() || accountId.isEmpty()) return;
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				HttpURLConnection conn = null;
				BufferedReader reader = null;
				try {
					URL url = new URL("https://api.cloudflare.com/client/v4/accounts/" + accountId + "/workers/subdomain");
					conn = (HttpURLConnection) url.openConnection();
					conn.setRequestMethod("GET");
					conn.setRequestProperty("Authorization", "Bearer " + token);
					
					final int code = conn.getResponseCode();
					if (code >= 200 && code < 300) {
						InputStream in = new BufferedInputStream(conn.getInputStream());
						reader = new BufferedReader(new InputStreamReader(in));
						StringBuilder sb = new StringBuilder();
						String line;
						while ((line = reader.readLine()) != null) sb.append(line);
						final String json = sb.toString();
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								try {
									JSONObject obj = new JSONObject(json);
									if (obj.optBoolean("success", false)) {
										JSONObject result = obj.optJSONObject("result");
										if (result != null) {
											String subdomain = result.optString("subdomain", "");
											if (!subdomain.isEmpty()) {
												mode.edit().putString("cf_subdomain", subdomain).commit();
												tvSubdomain.setText(subdomain + ".workers.dev");
												tvSubdomain.setVisibility(View.VISIBLE);
											}
										}
									}
								} catch (Exception ignored) {}
							}
						});
					}
				} catch (Exception ignored) {
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

	private void updateStatusDot(boolean online) {
		if (viewStatusDot != null) {
			viewStatusDot.setBackgroundResource(online ? getFnmods("dot_status_online", "drawable") : getFnmods("dot_status_offline", "drawable"));
		}
	}
}