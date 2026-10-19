package es.bvis.btconnect;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

// Connects or disconnects a paired Bluetooth audio device over A2DP without touching the screen:
//   am broadcast -f 0x20 -n es.bvis.btconnect/.ConnectReceiver --es mac AA:BB:.. [--es action disconnect]
// BluetoothA2dp.connect/disconnect are hidden in Android 10, hence reflection; Android 11 closed them to apps.
public class ConnectReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        final PendingResult result = goAsync();
        final String mac = intent.getStringExtra("mac");
        final String action = "disconnect".equals(intent.getStringExtra("action")) ? "disconnect" : "connect";
        final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null || !adapter.isEnabled() || mac == null || !BluetoothAdapter.checkBluetoothAddress(mac)) {
            done(result, "ERROR bluetooth off or bad mac");
            return;
        }
        final BluetoothDevice device = adapter.getRemoteDevice(mac);
        boolean bound = adapter.getProfileProxy(context, new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                String out;
                try {
                    Object ok = BluetoothA2dp.class.getMethod(action, BluetoothDevice.class).invoke(proxy, device);
                    out = action + "=" + ok;
                } catch (Throwable t) {
                    out = "ERROR " + (t.getCause() != null ? t.getCause() : t);
                }
                adapter.closeProfileProxy(BluetoothProfile.A2DP, proxy);
                done(result, out);
            }

            @Override
            public void onServiceDisconnected(int profile) {
            }
        }, BluetoothProfile.A2DP);
        if (!bound) done(result, "ERROR no A2DP proxy");
    }

    private static void done(PendingResult result, String data) {
        result.setResultCode(data.startsWith("ERROR") ? 1 : 0);
        result.setResultData(data);
        result.finish();
    }
}
