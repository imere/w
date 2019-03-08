package com.w.im;

import android.os.Bundle;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.inuker.bluetooth.library.BluetoothClient;
import com.inuker.bluetooth.library.beacon.Beacon;
import com.inuker.bluetooth.library.connect.listener.BleConnectStatusListener;
import com.inuker.bluetooth.library.connect.listener.BluetoothStateListener;
import com.inuker.bluetooth.library.connect.options.BleConnectOptions;
import com.inuker.bluetooth.library.connect.response.BleConnectResponse;
import com.inuker.bluetooth.library.connect.response.BleNotifyResponse;
import com.inuker.bluetooth.library.connect.response.BleReadResponse;
import com.inuker.bluetooth.library.connect.response.BleUnnotifyResponse;
import com.inuker.bluetooth.library.connect.response.BleWriteResponse;
import com.inuker.bluetooth.library.model.BleGattCharacter;
import com.inuker.bluetooth.library.model.BleGattDescriptor;
import com.inuker.bluetooth.library.model.BleGattProfile;
import com.inuker.bluetooth.library.model.BleGattService;
import com.inuker.bluetooth.library.receiver.listener.BluetoothBondListener;
import com.inuker.bluetooth.library.search.SearchRequest;
import com.inuker.bluetooth.library.search.SearchResult;
import com.inuker.bluetooth.library.search.response.SearchResponse;
import com.inuker.bluetooth.library.utils.BluetoothLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.inuker.bluetooth.library.Constants.BOND_BONDED;
import static com.inuker.bluetooth.library.Constants.BOND_BONDING;
import static com.inuker.bluetooth.library.Constants.BOND_NONE;
import static com.inuker.bluetooth.library.Constants.REQUEST_CANCELED;
import static com.inuker.bluetooth.library.Constants.REQUEST_DENIED;
import static com.inuker.bluetooth.library.Constants.REQUEST_EXCEPTION;
import static com.inuker.bluetooth.library.Constants.REQUEST_FAILED;
import static com.inuker.bluetooth.library.Constants.REQUEST_READ;
import static com.inuker.bluetooth.library.Constants.REQUEST_SUCCESS;
import static com.inuker.bluetooth.library.Constants.REQUEST_TIMEDOUT;
import static com.inuker.bluetooth.library.Constants.REQUEST_WRITE;
import static com.inuker.bluetooth.library.Constants.STATUS_CONNECTED;
import static com.inuker.bluetooth.library.Constants.STATUS_DISCONNECTED;

public class WatchActivity extends AppCompatActivity implements ListAdapter.OnItemClickListener {

    private TextView mBattery;
    private TextView mTemperature;
    private TextView mPH;

    private TextView mLog;

    private RecyclerView mList;
    private ListAdapter mListAdapter;

    private HashMap<String, String> mMacMap = new HashMap<>(0);
    private HashSet<UUID> mServiceUUIDSet = new HashSet<>(0);
    private HashSet<BleGattCharacter> mCharacterSet = new HashSet<>(0);
    private HashSet<BleGattDescriptor> mDescriptorSet = new HashSet<>(0);

    private HashMap<UUID, HashSet<BleGattCharacter>> mUUIDToCharacter = new HashMap<>(0);
    private HashMap<BleGattCharacter, HashSet<BleGattDescriptor>> mCharacterToDescriptor = new HashMap<>(0);

    private BluetoothClient mClient;
    private final BluetoothStateListener mBluetoothStateListener = new BluetoothStateListener() {
        @Override
        public void onBluetoothStateChanged(boolean openOrClosed) {
            if (openOrClosed) {
                startSearch();
            }
        }

    };
    private final BluetoothBondListener mBluetoothBondListener = new BluetoothBondListener() {
        @Override
        public void onBondStateChanged(String mac, int bondState) {
            switch (bondState) {
                case BOND_NONE:
                    stopSearch();
                    startSearch();
                    logToView("bond none", mac);
                    break;
                case BOND_BONDING:
                    logToView("bonding", mac);
                    break;
                case BOND_BONDED:
                    logToView("bonded", mac);
                    connect(mac);
                    break;
            }
        }
    };
    private final BleConnectStatusListener mBleConnectStatusListener = new BleConnectStatusListener() {
        @Override
        public void onConnectStatusChanged(String mac, int status) {
            switch (status) {
                case STATUS_CONNECTED:
                    logToView("connected", mac);
                    for (UUID serviceUUID : mUUIDToCharacter.keySet()) {
                        for (BleGattCharacter character : Objects.requireNonNull(mUUIDToCharacter.get(serviceUUID))) {
                            readCharacteristic(mac, serviceUUID, character.getUuid());
                            WatchActivity.this.notify(mac, serviceUUID, character.getUuid());
                        }
                    }
                    break;
                case STATUS_DISCONNECTED:
                    logToView("disconnected", mac);
                    connect(mac);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watch);

        initView();

        if (mClient == null) {
            mClient = new BluetoothClient(this);
        }
        mClient.registerBluetoothStateListener(mBluetoothStateListener);
        mClient.registerBluetoothBondListener(mBluetoothBondListener);
        if (mClient.isBluetoothOpened()) {
            startSearch();
        } else {
            mClient.openBluetooth();
        }
    }

    private void initView() {
        mBattery = findViewById(R.id.battery);
        mTemperature = findViewById(R.id.temperature);
        mPH = findViewById(R.id.ph);

        mLog = findViewById(R.id.log);
        mLog.setMovementMethod(ScrollingMovementMethod.getInstance());

        mList = findViewById(R.id.list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        mList.setLayoutManager(layoutManager);
        mListAdapter = new ListAdapter(this, this);
        mList.setAdapter(mListAdapter);
    }

    @Override
    public void onClick(View view, int position) {
        mLog.setText("");
        mLog.scrollTo(0, 0);
        connect(mListAdapter.getAll().get(position).getContent());
        Toast.makeText(this, "connecting " + mListAdapter.getAll().get(position).getTitle() + ": " + mListAdapter.getAll().get(position).getContent(), Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopSearch();
        mClient.unregisterBluetoothStateListener(mBluetoothStateListener);
        mClient.unregisterBluetoothBondListener(mBluetoothBondListener);
    }

    private void logToView(final String t, final String c) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mLog.append(String.format("%-30s %s\n", t + ":", c));
                mLog.scrollTo(0, mLog.getLineCount() * mLog.getLineHeight() - mLog.getHeight());
            }
        }, 0);
    }

    private void logToConsole(final String s) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                BluetoothLog.d(s);
            }
        }, 0);
    }

    private void startSearch() {
        SearchRequest request = new SearchRequest.Builder()
                .searchBluetoothLeDevice(3000, 3)   // 先扫BLE设备3次，每次3s
                .searchBluetoothClassicDevice(4000) // 再扫经典蓝牙4s
                .searchBluetoothLeDevice(3000)      // 再扫BLE设备3s
                .build();
        mClient.search(request, new SearchResponse() {
            @Override
            public void onSearchStarted() {
                logToView("search", "start");
                logToView("search", "等待搜索完成后再点击列表项连接");
            }

            @Override
            public void onDeviceFounded(SearchResult device) {
                Beacon beacon = new Beacon(device.scanRecord);
                List<Item> l = new ArrayList<>(0);
                mMacMap.put(device.getAddress(), device.getName());
                for (String mac : mMacMap.keySet()) {
                    l.add(new Item(mMacMap.get(mac), mac));
                }
                mListAdapter.setAll(l);
//                logToConsole(String.format("beacon for %s\n%s", device.getAddress(), beacon.toString()));
            }

            @Override
            public void onSearchStopped() {
                logToView("search", "stop");
            }

            @Override
            public void onSearchCanceled() {
                logToView("search", "cancel");
            }
        });
    }

    private void stopSearch() {
        mClient.stopSearch();
    }

    private void connect(String mac) {
        if (!mClient.isBluetoothOpened()) return;
        BleConnectOptions options = new BleConnectOptions.Builder()
                .setConnectRetry(2)   // 连接如果失败重试2次
                .setConnectTimeout(20000)   // 连接超时20s
                .setServiceDiscoverRetry(2)  // 发现服务如果失败重试2次
                .setServiceDiscoverTimeout(20000)  // 发现服务超时20s
                .build();
        mClient.registerConnectStatusListener(mac, mBleConnectStatusListener);
        mClient.connect(mac, options, new BleConnectResponse() {
            @Override
            public void onResponse(int code, BleGattProfile profile) {
                switch (code) {
                    case REQUEST_SUCCESS:
                        logToView("connect", "request success");
                        break;
                    case REQUEST_READ:
                        logToView("connect", "request read");
                        break;
                    case REQUEST_WRITE:
                        logToView("connect", "request write");
                        break;
                    case REQUEST_TIMEDOUT:
                        logToView("connect", "request timeout");
                        break;
                    case REQUEST_DENIED:
                        logToView("connect", "request denied");
                        break;
                    case REQUEST_EXCEPTION:
                        logToView("connect", "request exception");
                        break;
                    case REQUEST_FAILED:
                        logToView("connect", "request failed");
                        break;
                    default:
                        logToView("connect", "unhandled response");
                        break;
                }
                parseProfile(profile);
            }
        });
    }

    private void parseProfile(BleGattProfile profile) {
        reset();

        if (profile != null) {

            HashSet<BleGattService> serviceSet = new HashSet<>(0);
            HashSet<BleGattCharacter> characterSet = new HashSet<>(0);
            HashSet<BleGattDescriptor> descriptorSet = new HashSet<>(0);

            serviceSet.addAll(profile.getServices());

            for (BleGattService service : serviceSet) {

                mServiceUUIDSet.add(service.getUUID());

                characterSet.clear();
                characterSet.addAll(service.getCharacters());
                mUUIDToCharacter.put(service.getUUID(), characterSet);

                for (BleGattCharacter character : characterSet) {

                    mCharacterSet.add(character);

                    descriptorSet.clear();
                    descriptorSet.addAll(character.getDescriptors());
                    mCharacterToDescriptor.put(character, descriptorSet);

                    mDescriptorSet.addAll(descriptorSet);

                }
            }

        }
    }

    private void reset() {
        mMacMap.clear();
        mServiceUUIDSet.clear();
        mUUIDToCharacter.clear();
        mCharacterToDescriptor.clear();
    }

    private void readCharacteristic(String mac, UUID serviceUUID, final UUID characterUUID) {
        mClient.read(mac, serviceUUID, characterUUID, new BleReadResponse() {
            @Override
            public void onResponse(int code, byte[] data) {
                String cid = characterUUID.toString();
                switch (code) {
                    case REQUEST_SUCCESS:
                    case REQUEST_READ:
                    case REQUEST_WRITE:
                        logToView("read characteristic", cid + " success");
                        logToView("characteristic value", new String(data));
                        break;
                    case REQUEST_CANCELED:
                        logToView("read characteristic", cid + " canceled");
                        break;
                    case REQUEST_TIMEDOUT:
                        logToView("read characteristic", cid + " timeout");
                        break;
                    case REQUEST_DENIED:
                        logToView("read characteristic", cid + " denied");
                        break;
                    case REQUEST_EXCEPTION:
                        logToView("read characteristic", cid + " exception");
                        break;
                    case REQUEST_FAILED:
                        logToView("read characteristic", cid + " failed");
                        break;
                    default:
                        logToView("read characteristic", cid + " unhandled");
                        break;
                }
            }
        });
    }

    private void writeCharacteristic(String mac, UUID serviceUUID, final UUID characterUUID, byte[] bytes) {
//        byte[]不能超过20字节，如果超过了需要自己分成几次写。建议的办法是第一个byte放剩余要写的字节的长度
        mClient.write(mac, serviceUUID, characterUUID, bytes, new BleWriteResponse() {
            @Override
            public void onResponse(int code) {
                String cid = characterUUID.toString();
                switch (code) {
                    case REQUEST_SUCCESS:
                    case REQUEST_READ:
                    case REQUEST_WRITE:
                        logToView("write characteristic", cid + " success");
                        break;
                    case REQUEST_TIMEDOUT:
                        logToView("write characteristic", cid + " timeout");
                        break;
                    case REQUEST_DENIED:
                        logToView("write characteristic", cid + " denied");
                        break;
                    case REQUEST_EXCEPTION:
                        logToView("write characteristic", cid + " exception");
                        break;
                    case REQUEST_FAILED:
                        logToView("write characteristic", cid + " failed");
                        break;
                    default:
                        logToView("write characteristic", cid + " unhandled");
                        break;
                }
            }
        });
    }

    private void readDescriptor(String mac, UUID serviceUUID, UUID characterUUID, final UUID descriptorUUID) {
        mClient.readDescriptor(mac, serviceUUID, characterUUID, descriptorUUID, new BleReadResponse() {
            @Override
            public void onResponse(int code, byte[] data) {
                String did = descriptorUUID.toString();
                switch (code) {
                    case REQUEST_SUCCESS:
                    case REQUEST_READ:
                    case REQUEST_WRITE:
                        logToView("read descriptor", did + " success");
                        logToView("descriptor value", new String(data));
                        break;
                    case REQUEST_TIMEDOUT:
                        logToView("read descriptor", did + " timeout");
                        break;
                    case REQUEST_DENIED:
                        logToView("read descriptor", did + " denied");
                        break;
                    case REQUEST_EXCEPTION:
                        logToView("read descriptor", did + " exception");
                        break;
                    case REQUEST_FAILED:
                        logToView("read descriptor", did + " failed");
                        break;
                    default:
                        logToView("read descriptor", did + " unhandled");
                        break;
                }
            }
        });
    }

    private void writeDescriptor(String mac, UUID serviceUUID, UUID characterUUID, UUID descriptorUUID, byte[] bytes) {
        mClient.writeDescriptor(mac, serviceUUID, characterUUID, descriptorUUID, bytes, new BleWriteResponse() {
            @Override
            public void onResponse(int code) {

            }
        });
    }

    private void notify(String mac, UUID serviceUUID, final UUID characterUUID) {
        mClient.notify(mac, serviceUUID, characterUUID, new BleNotifyResponse() {
            @Override
            public void onNotify(UUID service, UUID character, byte[] value) {
                logToView("onNotify", new String(value));
            }

            @Override
            public void onResponse(int code) {
                String cid = characterUUID.toString();
                switch (code) {
                    case REQUEST_SUCCESS:
                    case REQUEST_READ:
                    case REQUEST_WRITE:
                        logToView("notify", cid + " success");
                        break;
                    case REQUEST_CANCELED:
                        logToView("notify", cid + " canceled");
                        break;
                    case REQUEST_TIMEDOUT:
                        logToView("notify", cid + " timeout");
                        break;
                    case REQUEST_DENIED:
                        logToView("notify", cid + " denied");
                        break;
                    case REQUEST_EXCEPTION:
                        logToView("notify", cid + " exception");
                        break;
                    case REQUEST_FAILED:
                        logToView("notify", cid + " failed");
                        break;
                    default:
                        logToView("notify", cid + " unhandled");
                        break;
                }
            }
        });
    }

    private void unNotify(String mac, UUID serviceUUID, final UUID characterUUID) {
        mClient.unnotify(mac, serviceUUID, characterUUID, new BleUnnotifyResponse() {
            @Override
            public void onResponse(int code) {
                String cid = characterUUID.toString();
                switch (code) {
                    case REQUEST_SUCCESS:
                    case REQUEST_READ:
                    case REQUEST_WRITE:
                        logToView("unNotify", cid + " success");
                        break;
                    case REQUEST_TIMEDOUT:
                        logToView("unNotify", cid + " timeout");
                        break;
                    case REQUEST_DENIED:
                        logToView("unNotify", cid + " denied");
                        break;
                    case REQUEST_EXCEPTION:
                        logToView("unNotify", cid + " exception");
                        break;
                    case REQUEST_FAILED:
                        logToView("unNotify", cid + " failed");
                        break;
                }
            }
        });
    }

}
