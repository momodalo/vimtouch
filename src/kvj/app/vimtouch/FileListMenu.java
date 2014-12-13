package kvj.app.vimtouch;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.ipaulpro.afilechooser.FileChoosedListener;
import com.ipaulpro.afilechooser.FileListFragment;
import com.ipaulpro.afilechooser.utils.FileInfo;

import java.io.File;

public class FileListMenu implements VimTouch.SlidingMenuInterface, FileChoosedListener {

    private VimTouch mVim = null;
    private String mLastDir = null;

    FileListMenu(VimTouch vim) {
        mVim = vim;
    }

    public Fragment getFragment() {
        mLastDir = Environment.getExternalStorageDirectory().getAbsolutePath();
        FileListFragment explorerFragment = FileListFragment.newInstance(mLastDir, true);
        explorerFragment.setFileChoosedListener(this);
        return (Fragment) explorerFragment;
    }

    public void onOpen() {
        showDirectory(mLastDir);
    }

    public void onClose() {
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_save) {
            if (Exec.isInsertMode()) {
                mVim.write(27);
            }
            mVim.write(":w " + mLastDir + "/");
            return true;
        } else if (id == R.id.menu_toggle_soft_keyboard) {
            Exec.doCommand("cd " + mLastDir);
            Toast.makeText(mVim, ":cd " + mLastDir, Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.menu_ESC) {
            return true;
        }
        return false;
    }

    public boolean onNavigationItemSelected(int pos, long id) {
        showDirectory(mVim.getTabAdapter().getItem(pos).toString());
        return true;
    }

    private void showDirectory(String path) {
        FileListFragment explorerFragment = FileListFragment.newInstance(path, true);
        explorerFragment.setFileChoosedListener(this);
        mVim.setSlidingMenuFragment((Fragment) explorerFragment);
    }

    public void onFileSelected(FileInfo file) {
        if (file != null) {
            String path = file.getFile().getAbsolutePath();
            String ext = path.substring(file.getFile().getName().lastIndexOf('.') + 1);

            if (file.getFile().isDirectory()) {
                showDirectory(path);
            } else if (ext.equals("vrz")) {
                mLastDir = path;
                AlertDialog.Builder builder = new AlertDialog.Builder(mVim);
                builder.setMessage(R.string.restore_message)
                    .setTitle(R.string.restore_title)
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Intent
                                intent =
                                new Intent(mVim.getApplicationContext(), InstallProgress.class);
                            intent.setData(Uri.parse("file://" + mLastDir));
                            mVim.startActivityForResult(intent, VimTouch.REQUEST_VRZ);
                        }
                    });

                AlertDialog dialog = builder.create();
                dialog.show();

            } else {
                mVim.openNewFile(path);
            }
        } else {
            Toast.makeText(mVim, R.string.error_selecting_file, Toast.LENGTH_SHORT).show();
        }
    }
}
