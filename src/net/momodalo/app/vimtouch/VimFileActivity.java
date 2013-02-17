package net.momodalo.app.vimtouch;

import android.app.Activity;
import com.lamerman.FileDialog;
import android.os.Bundle;
import android.os.Environment;
import android.content.Intent;
import java.io.File;
import android.net.Uri;
import android.util.Log;

public class VimFileActivity extends Activity{
    public static final String OPEN_TYPE = "open_type";

    public static final int FILE_TABNEW = 1;
    public static final int FILE_NEW = 2;
    public static final int FILE_VNEW = 3;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        int opentype = getIntent().getExtras().getInt(OPEN_TYPE, FILE_TABNEW);

        Intent intent = new Intent(getBaseContext(), VimFileDialog.class);
        intent.putExtra(FileDialog.START_PATH, ".");
                                            
        //can user select directories or not
        intent.putExtra(FileDialog.CAN_SELECT_DIR, false);
                                                            
        startActivityForResult(intent, opentype);
	}

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            String filePath = data.getStringExtra(FileDialog.RESULT_PATH);
            Intent intent = new Intent(getBaseContext(), VimTouch.class);
            File file = new File(filePath.replace(" ", "\\ "));
            intent.setData(Uri.fromFile(file));
            intent.putExtra(OPEN_TYPE, requestCode);
            startActivity(intent);
        }
        finish();
    }

}
