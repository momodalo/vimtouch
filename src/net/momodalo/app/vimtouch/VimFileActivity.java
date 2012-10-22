package net.momodalo.app.vimtouch;

import android.app.Activity;
import com.lamerman.FileDialog;
import android.os.Bundle;
import android.content.Intent;
import java.io.File;
import android.net.Uri;
import android.util.Log;

public class VimFileActivity extends Activity{
    private static final int REQUEST_OPEN = 1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        Intent intent = new Intent(getBaseContext(), VimFileDialog.class);
        intent.putExtra(FileDialog.START_PATH, "/sdcard");
                                            
        //can user select directories or not
        intent.putExtra(FileDialog.CAN_SELECT_DIR, false);
                                                            
        startActivityForResult(intent, REQUEST_OPEN);
	}

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_OPEN){
            if (resultCode == Activity.RESULT_OK) {
                String filePath = data.getStringExtra(FileDialog.RESULT_PATH);
                Intent intent = new Intent(getBaseContext(), VimTouch.class);
                File file = new File(filePath.replace(" ", "\\ "));
                intent.setData(Uri.fromFile(file));
                startActivity(intent);
            }
        }
        finish();
    }

}
