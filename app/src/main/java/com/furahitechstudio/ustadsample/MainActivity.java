package com.furahitechstudio.ustadsample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import com.furahitechstudio.ustadsample.manager.BluetoothManagerSharedAndroid;
import com.furahitechstudio.ustadsample.utils.LogWrapper;

import static com.furahitechstudio.ustadsample.manager.BluetoothManagerShared.ENTRY_STATUS_REQUEST;

public class MainActivity extends AppCompatActivity {

  private String sample_ops_ids =
      "dce655f2-34f0-469c-b890-a910039b0afc,c9d07319-2ab0-4a53-82cb-02370f5b8699,f912c86a-7f3b-406b-aef9-816e47bc00c0,2bf51d84-c1b5-4b48-b451-b5f8500579d3,4ff2aa2e-4940-4280-8027-bb77258982f0,7636a40f-0db3-478a-bef6-974f950f2f21,66e16057-c211-4080-8787-55927d244244,0a273c2d-d879-46f7-bd0d-9682328958ea,a33aeae5-224a-46c8-8ee8-363a86804a53,7ede402b-b74f-416d-8350-da3a510bc85e,0a1bc4f4-9eb4-448e-901b-233969b9b885,4bb7e255-c400-45b2-94b8-be8de0d9fa8a,7379b522-8d77-4ba7-9a2c-59d9f776e519,63762f22-7af0-49a7-a51e-cf7fb1c35d5e29c8b873-0d73-486a-924f-42649197feab,c7ebd6c1-3b77-4cd9-a139-9173d36bf256,a1b596cb-0fda-4cdc-bb5e-9bffa4ea2386,cc4a536b-7aec-4ba3-93d4-de9eb3a42812,a398930c-9e04-4628-9694-8b3e1e1a2a7d,ee497f1f-8b40-44ed-bd8c-033d2ce1302d,8d3e708a-1083-48c5-b1cc-4dfc5c858968,43d236f7-3964-40f2-86ca-5810115d6386,4a288b58-5605-4666-bebd-f6c17712ed37,d2a50b39-2183-4801-8508-542371395082,ed0597fc-6329-470b-b791-49f10011112c,198d920e-74ab-428a-93c8-d0d4a31031c2,a2ebd54c-7f18-4cf5-980d-b3702a1d856d,968d8bf4-f230-445e-a1b9-41b79e0d3969,e7dab6d1-d67a-4dc2-891d-13341c7055d6,df85d05e-cbf1-4c54-9574-47d14f4e257a,bfdb83fe-4a58-4210-bd63-ebd928203db8,825e77a5-6889-4438-a765-894c94decdd0,7c5f295a-b301-4619-a17b-9c0fea5e29ee,29f43c01-3a88-435b-b835-943664d8aa02, 994cf488-a8b8-4d24-aec5-806edb0e4bf6,8a173c1f-e812-48d2-8441-14958f48a216,389bb226-bf90-46ab-a3ad-612d8acb4514,b09e7068-834a-49c6-bbef-cc26d302cea4,8230af69-f6ad-4174-85d9-17806f214d43,cd26815b-25fd-461b-99ea-5413c32e1092,920fb2a6-f898-4b08-a959-ada3d069c9e8,318ddfd5-09b7-44b0-9dbd-5c0294793c50,01edee2b-1670-4782-83f5-2468d494f51f,20b635ec-89b4-46be-8172-ebeb157ca171,e8e2f13a-9229-437e-a511-6031d25bb5d1,3d663e70-abf3-48d5-b9dd-c0a7f33a0b4f,c67aed18-c392-4dbb-8587-1920741f4960,c1d9d6a5-f199-48ce-a4e8-baa84f23f4dd,d51b79ae-91bc-4b30-9881-7bc1617cc242,269929a3-8c0b-4dc2-94b8-caf02f4767b7";

  private BluetoothManagerSharedAndroid bluetoothImpl;
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    Button startServer = findViewById(R.id.btn_start_server);
    Button startClient = findViewById(R.id.btn_start_client);
    final Button endConnection = findViewById(R.id.btn_end_connection);
    final Button sendMessage = findViewById(R.id.btn_send_message);

    final Button startConnection = findViewById(R.id.btn_start_connection);

    bluetoothImpl = new BluetoothManagerSharedAndroid(this);

    startServer.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        if(bluetoothImpl.isBluetoothEnabled() && bluetoothImpl.isBleCapable()
            && bluetoothImpl.canDeviceAdvertise()){
          bluetoothImpl.startAdvertising();
        }else{
          LogWrapper.log(true, "There is an error with the device capabilities");
        }
      }
    });
    startClient.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        if(bluetoothImpl.isBluetoothEnabled() && bluetoothImpl.isBleCapable()){
          bluetoothImpl.startScanning();
          startConnection.setVisibility(View.VISIBLE);
          sendMessage.setVisibility(View.VISIBLE);
          endConnection.setVisibility(View.VISIBLE);
        }else{
          LogWrapper.log(true, "Device is not BLE capable");
        }
      }
    });


    startConnection.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        bluetoothImpl.connectToServerDevice(bluetoothImpl.getAvailableNodes()
            .get(bluetoothImpl.getAvailableNodes().size()-1));
      }
    });

    endConnection.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        bluetoothImpl.stopService();
      }
    });

    sendMessage.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        LogWrapper.log(false, "sending "
            +sample_ops_ids.getBytes().length+" bytes"+" "
            +(sample_ops_ids.split(",")).length+" ids");
        bluetoothImpl.sendRequestToSuperNode(sample_ops_ids,ENTRY_STATUS_REQUEST);
      }
    });


  }

  @Override protected void onDestroy() {
    super.onDestroy();
    bluetoothImpl.destroy();
  }
}
