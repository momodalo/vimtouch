package net.momodalo.app.vimtouch;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.content.Intent;
import java.io.File;
import android.net.Uri;
import android.util.Log;

import com.ipaulpro.afilechooser.utils.FileUtils;

public class VimFileActivity extends Activity{
    public static final String OPEN_TYPE = "open_type";
    public static final String OPEN_PATH = "open_path";

    public static final int FILE_TABNEW = 1;
    public static final int FILE_NEW = 2;
    public static final int FILE_VNEW = 3;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        int opentype = getIntent().getExtras().getInt(OPEN_TYPE, FILE_TABNEW);
        String path = getIntent().getExtras().getString(OPEN_PATH);

        String title = "";
        if(opentype == FILE_TABNEW) title = getString(R.string.tabnew_file);
        if(opentype == FILE_NEW) title = getString(R.string.new_file);
        if(opentype == FILE_VNEW) title = getString(R.string.vnew_file);
		Intent target = FileUtils.createGetContentIntent();
		Intent intent = Intent.createChooser(target, title);
		startActivityForResult(intent, opentype);
	}

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            final Uri uri = data.getData();

            try {
                final File file = FileUtils.getFile(uri);
                Intent intent = new Intent(getBaseContext(), VimTouch.class);
                intent.setData(Uri.fromFile(file));
                intent.putExtra(OPEN_TYPE, requestCode);
                startActivity(intent);
            }catch (Exception e){
            }
        }
        finish();
    }

}
