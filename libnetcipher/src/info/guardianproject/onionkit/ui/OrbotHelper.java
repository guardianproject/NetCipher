
package info.guardianproject.onionkit.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;

import info.guardianproject.onionkit.R;

public class OrbotHelper {

    private final static int REQUEST_CODE_STATUS = 100;

    public final static String ORBOT_PACKAGE_NAME = "org.torproject.android";
    public final static String ORBOT_MARKET_URI = "market://details?id=" + ORBOT_PACKAGE_NAME;
    public final static String ORBOT_FDROID_URI = "https://f-droid.org/repository/browse/?fdid=org" + ORBOT_PACKAGE_NAME;
    public final static String ORBOT_PLAY_URI = "https://play.google.com/store/apps/details?id=" + ORBOT_PACKAGE_NAME;

    public final static String TOR_BIN_PATH = "/data/data/" + ORBOT_PACKAGE_NAME + "/app_bin/tor";

    public final static String ACTION_START_TOR = "org.torproject.android.START_TOR";
    public final static String ACTION_REQUEST_HS = "org.torproject.android.REQUEST_HS_PORT";
    public final static int HS_REQUEST_CODE = 9999;

    private Context mContext = null;

    public OrbotHelper(Context context)
    {
        mContext = context;
    }

    public boolean isOrbotRunning()
    {
        int procId = TorServiceUtils.findProcessId(TOR_BIN_PATH);

        return (procId != -1);
    }

    public boolean isOrbotInstalled()
    {
        return isAppInstalled(ORBOT_PACKAGE_NAME);
    }

    private boolean isAppInstalled(String uri) {
        PackageManager pm = mContext.getPackageManager();
        boolean installed = false;
        try {
            pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
            installed = true;
        } catch (PackageManager.NameNotFoundException e) {
            installed = false;
        }
        return installed;
    }

    public void promptToInstall(Activity activity)
    {
        // show dialog - install from market, f-droid or direct APK
        showDownloadDialog(activity, activity.getString(R.string.install_orbot_),
                activity.getString(R.string.you_must_have_orbot),
                activity.getString(R.string.yes), activity.getString(R.string.no));
    }

    private static AlertDialog showDownloadDialog(final Activity activity,
            CharSequence stringTitle, CharSequence stringMessage, CharSequence stringButtonYes,
            CharSequence stringButtonNo) {
        AlertDialog.Builder downloadDialog = new AlertDialog.Builder(activity);
        downloadDialog.setTitle(stringTitle);
        downloadDialog.setMessage(stringMessage);
        downloadDialog.setPositiveButton(stringButtonYes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                try {
                    intent.setData(Uri.parse(ORBOT_MARKET_URI));
                    activity.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    intent.setData(Uri.parse(ORBOT_FDROID_URI));
                    activity.startActivity(intent);
                }
            }
        });
        downloadDialog.setNegativeButton(stringButtonNo, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        return downloadDialog.show();
    }

    public void requestOrbotStart(final Activity activity)
    {

        AlertDialog.Builder downloadDialog = new AlertDialog.Builder(activity);
        downloadDialog.setTitle(R.string.start_orbot_);
        downloadDialog
                .setMessage(R.string.orbot_doesn_t_appear_to_be_running_would_you_like_to_start_it_up_and_connect_to_tor_);
        downloadDialog.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                activity.startActivityForResult(getOrbotStartIntent(), 1);
            }
        });
        downloadDialog.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        downloadDialog.show();

    }

    public void requestHiddenServiceOnPort(Activity activity, int port)
    {
        Intent intent = new Intent(ACTION_REQUEST_HS);
        intent.setPackage(ORBOT_PACKAGE_NAME);
        intent.putExtra("hs_port", port);

        activity.startActivityForResult(intent, HS_REQUEST_CODE);
    }

    public static Intent getOrbotStartIntent() {
        Intent intent = new Intent(ACTION_START_TOR);
        intent.setPackage(ORBOT_PACKAGE_NAME);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }
}
