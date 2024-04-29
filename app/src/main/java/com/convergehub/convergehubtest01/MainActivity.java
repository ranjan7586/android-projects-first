package com.convergehub.convergehubtest01;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

import android.os.Environment;
import android.content.pm.PackageManager;




public class MainActivity extends AppCompatActivity {
    private boolean fileChooserAppears=false;
    private boolean errorRecieved=false;
    private static final int REQUEST_CAPTURE_IMAGE = 100;
    private static final int FILE_CHOOSER_RESULT_CODE = 1;
    private ValueCallback<Uri[]> mUploadMessage;
    private WebView webView;
    private ProgressBar progressBar;
    boolean noRefreshModuleName=false;

    private RelativeLayout relativeLayoutNoInternet;
    private RelativeLayout parentLayout;
    private Button retryBtn;
    private SwipeRefreshLayout refreshLayout;
    private String mCameraPhotoPath;
    private static final int REQUEST_PERMISSION_CODE = 10;
    private static final String ONESIGNAL_APP_ID = "74ff4315-4b58-4753-90b9-254f06067f97";
    int firstLoaded=0;
    boolean hasInternetConnection;
    boolean hasInternetChanged;
    private static final int REQUEST_PERMISSION_CODE_CAMERA=90;
    private RelativeLayout somethingwentwrong;

    private Button somethingwentwrongbutton;

    boolean somewrongerror = false;

    private  BroadcastReceiver connectivityReceiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            internetCheck();

        }
    };
    private void openAppSettings() {
        Intent intent = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS");
        intent.setData(Uri.fromParts("package", getPackageName(), null));
        startActivity(intent);
    };

    private void showPermissionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder((Context)this);
        builder.setTitle("Allow Convergehub to use your phone's storage and camera both ?")
                .setMessage("This lets Convergehub store and access information like photos on your phone and its SD card and also able to capture photos from camera.\nTo enable this, click App Settings below and activate Storage and Camera under the Permissions menu.")
                .setPositiveButton("APP SETTINGS", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface param1DialogInterface, int param1Int) {
                MainActivity.this.openAppSettings();
            }
        }).setNegativeButton("NOT NOW", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface param1DialogInterface, int param1Int) {
                param1DialogInterface.dismiss();
            }
        });
        builder.create().show();
    }

    @SuppressLint({"MissingInflatedId", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        String[] arrayOfString;
        if (Build.VERSION.SDK_INT < 33) {
            arrayOfString = new String[] { "android.permission.READ_EXTERNAL_STORAGE", "android.permission.CAMERA" };
        } else {
            arrayOfString = new String[] { "android.permission.READ_MEDIA_IMAGES", "android.permission.CAMERA" };
        }
        if (ContextCompat.checkSelfPermission((Context)this, arrayOfString[0]) == -1 || ContextCompat.checkSelfPermission((Context)this, arrayOfString[1]) == -1) {
            requestPermissions(arrayOfString, 10);
        } else {
            Toast.makeText((Context)this, "The app has your camera and storage access", Toast.LENGTH_SHORT).show();
        }

        webView = findViewById(R.id.webview);
        progressBar=findViewById(R.id.progressBar);
        parentLayout=findViewById(R.id.parentlayout);
        refreshLayout=findViewById(R.id.refresh);
        webView.setWebViewClient(new MyWebViewClient());
        webView.loadUrl("https://app01.convergehub.com");
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webView.setVerticalScrollBarEnabled(false);
        webView.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View param1View) {
                return true;
            }
        });
        webView.setLongClickable(false);
        webView.setWebChromeClient(new WebChromeClient(){
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                mUploadMessage = filePathCallback;
                if(!checkFIleChooserPermission("upload")){
                    AlertDialog.Builder builder=new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage("This lets Convergehub store and access information like photos on your phone and its SD card")
                            .setTitle("Please allow Convergehub to access your both storage and camera")
                            .setPositiveButton("Enable", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    filePermissionAppear();
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.dismiss();
                                }
                            });
                    builder.create().show();
                    return super.onShowFileChooser(webView, filePathCallback, fileChooserParams);
                }
                else {
                    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                        File photoFile = null;
                        try {
                            photoFile = createImageFile();
                            takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath);
                        } catch (IOException ex) {
                            Log.e("TAG", "Unable to create Image File", ex);
                        }
                        if (photoFile != null) {
                            mCameraPhotoPath = "file:" + photoFile.getAbsolutePath();
                            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                        } else {
                            takePictureIntent = null;
                        }
                    }
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("image/*");
                    Intent[] intentArray;
                    if (takePictureIntent != null) {
                        intentArray = new Intent[]{takePictureIntent};
                    } else {
                        intentArray = new Intent[0];
                    }
                    Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                    chooserIntent.putExtra(Intent.EXTRA_INTENT, intent);
                    chooserIntent.putExtra(Intent.EXTRA_TITLE, getString(R.string.image_chooser));
                    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
                    startActivityForResult(chooserIntent, 1);
                    return true;
                }
            };
        });

        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                String fileName="";
                if(Objects.equals(mimetype, "application/octet-stream")){
                    String extension=contentDisposition.substring(contentDisposition.lastIndexOf(".")+1,contentDisposition.length()-1);
                    mimetype = "application/" + extension;

                    if (Arrays.asList("jpg", "jpeg", "png", "gif").contains(extension)) {
                        mimetype = "image/" + extension;
                    }
                    if (contentDisposition != null && contentDisposition.contains("filename=")) {
                        fileName=contentDisposition.substring(contentDisposition.lastIndexOf("filename=")+10,contentDisposition.length()-1);
                    }
                }
                else{
                    fileName = URLUtil.guessFileName(url, contentDisposition, mimetype);
                }
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                request.setMimeType(mimetype);
                String cookies = CookieManager.getInstance().getCookie(url);
                request.addRequestHeader("cookie", cookies);
                request.addRequestHeader("User-Agent", userAgent);
                request.setDescription("Downloading file...");
                request.setTitle(fileName);
                request.allowScanningByMediaScanner();
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

                DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                downloadManager.enqueue(request);
                Toast.makeText(MainActivity.this, "Download Started", Toast.LENGTH_SHORT).show();

            };

        });
        webView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                webView.evaluateJavascript("(function() { " +
                                "var element = document.querySelector('.dwbg');" +
                                "if (element) {" +
                                "   return true;" +
                                "} else {" +
                                "   return false;" +
                                "}})();",
                        value -> {
                            if (value != null) {
                                noRefreshModuleName= Boolean.parseBoolean(value);
                            }
                        });
                if(noRefreshModuleName){
                    refreshLayout.setEnabled(false);
                    noRefreshModuleName=false;
                }
                else{
                    refreshLayout.setEnabled(true);
                }

                return false;
            }
        });

        relativeLayoutNoInternet=findViewById(R.id.nointernet);
        retryBtn=findViewById(R.id.retrybtn);
        retryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Please turn on your network connectivity", Toast.LENGTH_SHORT).show();
                internetCheck();
            }
        });

        somethingwentwrong = (RelativeLayout) findViewById(R.id.somethingwenwrong);
        somethingwentwrongbutton = (Button) findViewById(R.id.somethingwenwrongbutton);

        somethingwentwrongbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Please turn on your network connectivity", Toast.LENGTH_SHORT).show();
                internetCheck();
            }
        });

        refreshLayout.setEnabled(false);

        webView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                refreshLayout.setEnabled(scrollY==0);
            }
        });

        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshLayout.setRefreshing(true);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        refreshLayout.setRefreshing(false);
                        webView.reload();
                    }
                },2000);
            }
        });
        refreshLayout.setColorSchemeColors(
                getResources().getColor(android.R.color.holo_blue_dark),
                getResources().getColor(android.R.color.holo_orange_dark),
                getResources().getColor(android.R.color.holo_green_dark),
                getResources().getColor(android.R.color.holo_red_dark),
                getResources().getColor(android.R.color.holo_blue_bright)
        );

    }
    private boolean checkFIleChooserPermission(String paramString) {
        if (Objects.equals(paramString, "upload")) {
            if (Build.VERSION.SDK_INT < 33 && ContextCompat.checkSelfPermission((Context)this, "android.permission.CAMERA") == 0 && ContextCompat.checkSelfPermission((Context)this, "android.permission.READ_EXTERNAL_STORAGE") == 0)
                return true;
            if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission((Context)this, "android.permission.CAMERA") == 0 && ContextCompat.checkSelfPermission((Context)this, "android.permission.READ_MEDIA_IMAGES") == 0)
                return true;
        } else if (Objects.equals(paramString, "download")) {
            if (Build.VERSION.SDK_INT < 33 && ContextCompat.checkSelfPermission((Context)this, "android.permission.READ_EXTERNAL_STORAGE") == 0)
                return true;
            if (Build.VERSION.SDK_INT >= 33)
                return true;
        }
        return false;
    }
    private void filePermissionAppear() {
        String[] arrayOfString;
        if (Build.VERSION.SDK_INT < 33) {
            arrayOfString = new String[] { "android.permission.READ_EXTERNAL_STORAGE", "android.permission.CAMERA" };
        } else {
            arrayOfString = new String[] { "android.permission.CAMERA", "android.permission.READ_MEDIA_IMAGES" };
        }
        ActivityCompat.requestPermissions((Activity)this, arrayOfString, 90);
        if (Build.VERSION.SDK_INT < 33 && Build.VERSION.SDK_INT > 30 && !ActivityCompat.shouldShowRequestPermissionRationale((Activity)this, "android.permission.CAMERA") && !ActivityCompat.shouldShowRequestPermissionRationale((Activity)this, "android.permission.READ_EXTERNAL_STORAGE"))
            showPermissionDialog();
        if (Build.VERSION.SDK_INT >= 33 && !ActivityCompat.shouldShowRequestPermissionRationale((Activity)this, "android.permission.CAMERA") && !ActivityCompat.shouldShowRequestPermissionRationale((Activity)this, "android.permission.READ_MEDIA_IMAGES"))
            showPermissionDialog();
        if (Build.VERSION.SDK_INT < 31 && !ActivityCompat.shouldShowRequestPermissionRationale((Activity)this, "android.permission.CAMERA") && !ActivityCompat.shouldShowRequestPermissionRationale((Activity)this, "android.permission.READ_EXTERNAL_STORAGE"))
            showPermissionDialog();
    }
    String imageFilePath;
    private File createImageFile() throws IOException {

        File imageStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "DirectoryNameHere");

        if (!imageStorageDir.exists()) {
            imageStorageDir.mkdirs();
        }
        imageStorageDir = new File(imageStorageDir + File.separator + "IMG_" + String.valueOf(System.currentTimeMillis()) + ".jpg");
        return imageStorageDir;
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(connectivityReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(connectivityReceiver);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != FILE_CHOOSER_RESULT_CODE || mUploadMessage == null) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }
        Uri[] result = null;
        if (resultCode == RESULT_OK) {
            if (data == null || data.getData() == null) {
                if (mCameraPhotoPath != null) {
                    result = new Uri[]{Uri.parse(mCameraPhotoPath)};
                }
            } else {
                String dataString = data.getDataString();
                if (dataString != null) {
                    result = new Uri[]{Uri.parse(dataString)};
                }
            }
        }
        mUploadMessage.onReceiveValue(result);
        mUploadMessage = null;
    }

    private class MyWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.startsWith("tel:")) {
                String str = url;
                if (url.contains("||"))
                    str = url.toString().substring(0, url.toString().length() - 8);
                Intent intent = new Intent("android.intent.action.DIAL", Uri.parse(str));
                MainActivity.this.startActivity(intent);
                webView.reload();
                return true;
            }
            return false;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            if(((Objects.equals(webView.getUrl(), "https://mobile.convergehub.app/dashboard")) ||
                    (Objects.equals(webView.getUrl(), "https://mobile.convergehub.app/"))) && (firstLoaded==0)) {
                firstLoaded++;
                if (progressBar != null) {
                    progressBar.setVisibility(View.VISIBLE);
                    webView.setVisibility(View.GONE);
                }
            }
            else if(hasInternetConnection){
                if (progressBar != null) {
                    progressBar.setVisibility(View.VISIBLE);
                    webView.setVisibility(View.GONE);
                }
                hasInternetConnection=false;
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
                webView.setVisibility(View.VISIBLE);
            }
            if(errorRecieved){
                internetCheck();
            }
            errorRecieved=false;
            if (Objects.equals(MainActivity.this.webView.getUrl(), "https://app01.convergehub.com/")) {
                webView.evaluateJavascript("function showLoginForm() {\n  $(\"#login_button\").hide(), $(\"#login_area\").show();\n  var e = $(\"html,body\");\n  e.animate({scrollTop: 0}, 0);\n}document.getElementsByClassName('login-bg')[0].style.backgroundColor=\"#D7D7D7\t\";let a = document.querySelectorAll('input');a.forEach((item)=>{\n    item.style.padding=\"0.5rem 0.875rem\";\n    item.style.border=\"1px solid #000\";\n    item.style.borderRadius=\"10px\"\n});document.getElementById('errormsg').style.color='#000';document.getElementsByTagName('p')[7].style.color=\"#000\"\n;document.getElementById('forgotPassword').style.color=\"#000\"\n", null);
                return;
            }
            if (Objects.equals(MainActivity.this.webView.getUrl(), "https://app01.convergehub.com/calendar")) {
                webView.evaluateJavascript("document.getElementsByClassName('menu-area')[4].style.background=\"#f12545\"\n;document.getElementById('nav').style.backgroundColor=\"#333333\"\n", null);
                return;
            }
            webView.evaluateJavascript("document.getElementsByClassName('menu-area')[2].style.backgroundColor=\"#f12545\"\n;document.getElementsByClassName('ui-page')[0].style.backgroundColor=\"#333333\"\n;document.getElementsByTagName('nav')[0].style.backgroundColor=\"#1C1C1C\"\n;document.querySelectorAll('.content-place .menu-style ul li a').forEach((i)=>{\n    i.style.setProperty(\"color\", \"#ffffff\", \"important\");\n});document.getElementsByClassName('copyright_1 ')[0].style.position=\"fixed\";\ndocument.getElementsByClassName('copyright_1 ')[0].style.bottom=\"2%\";\ndocument.getElementsByClassName('copyright_1 ')[0].style.width=\"100%\";document.getElementsByClassName('copyright_1 ')[0].style.setProperty(\"color\", \"#ffffff\", \"important\");\ndocument.getElementById(\"footer_panel\").style.backgroundColor=\"#f12545\"", null);
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);
            errorRecieved=true;
            if (error.getDescription().toString().equals("net::ERR_ADDRESS_UNREACHABLE") || error.getDescription().toString().equals("net::ERR_INTERNET_DISCONNECTED") || error.getDescription().toString().equals("net::ERR_TIMED_OUT") || error.getDescription().toString().equals("net::ERR_NAME_NOT_RESOLVED")) {
                MainActivity.this.somewrongerror = true;
                MainActivity.this.webView.setVisibility(View.GONE);
                MainActivity.this.somethingwentwrong.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onBackPressed(){
        ConnectivityManager connectivityManager=(ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        relativeLayoutNoInternet.setVisibility(View.GONE);

        if(webView.canGoBack()){
            webView.goBack();
        }
        else{
            super.onBackPressed();
        }
//        if(connectivityManager.getActiveNetworkInfo()==null){
//            webView.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    internetCheck();
//                }
//            });
//        }
    }
    @Override

    public void onRequestPermissionsResult(int i, String[] strArr, int[] iArr) {
        super.onRequestPermissionsResult(i, strArr, iArr);
        if (i == 10) {
            if (iArr.length > 0 && iArr[0] == 0 && iArr[1] == 0) {
                Toast.makeText(this, "The app has your camera and storage access", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Storage and Camera permissions are required to capture and upload images.", Toast.LENGTH_LONG).show();
            }
        } else if (i == REQUEST_PERMISSION_CODE_CAMERA && iArr.length > 0 && iArr[0] == 0 && iArr[1] == 0) {
            Toast.makeText(this, "The app has your camera and storage access", Toast.LENGTH_SHORT).show();
        }
    }
    public void internetCheck(){
        ConnectivityManager connectivityManager=(ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        hasInternetConnection=(connectivityManager.getActiveNetworkInfo()!=null);

        if(connectivityManager!=null){
            if(connectivityManager.getActiveNetworkInfo()!=null && connectivityManager.getActiveNetworkInfo().isConnected()){
                if(fileChooserAppears){
                    String checkUrl=webView.getUrl();
                    if(checkUrl.indexOf("https://mobile.convergehub.app/users/detail/")==-1){
                        if(checkUrl.indexOf("https://mobile.convergehub.app/login")>=0){
                            webView.loadUrl("https://mobile.convergehub.app/login");
                        }
                        else
                            webView.reload();
                    }
                    else{
                        webView.setVisibility(View.VISIBLE);
                    }
                    fileChooserAppears=false;
                }
                relativeLayoutNoInternet.setVisibility(View.GONE);
            }
            else {
                fileChooserAppears=true;
                webView.setVisibility(View.GONE);
                relativeLayoutNoInternet.setVisibility(View.VISIBLE);
            }
        }
        else {
            fileChooserAppears=true;
            webView.setVisibility(View.GONE);
            relativeLayoutNoInternet.setVisibility(View.VISIBLE);
        }
    }

}