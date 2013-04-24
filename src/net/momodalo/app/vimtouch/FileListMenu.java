package net.momodalo.app.vimtouch;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import android.support.v4.app.Fragment;

import java.io.File;

import com.ipaulpro.afilechooser.FileListFragment;
import com.ipaulpro.afilechooser.FileChoosedListener;

public class FileListMenu implements VimTouch.SlidingMenuInterface, FileChoosedListener {
    private VimTouch mVim = null;
    private String mLastDir = null;

    FileListMenu(VimTouch vim) {
        mVim = vim;
    }

    public Fragment getFragment(){
	    FileListFragment explorerFragment = FileListFragment.newInstance(".");
        explorerFragment.setFileChoosedListener(this);
        return (Fragment)explorerFragment;
    }

    public void onOpen(){
        String path = Exec.getcwd();
        showDirectory(path);
    }

    public void onClose(){
    }

    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();
        if (id == R.id.menu_save) {
            mVim.showContent();
            if(Exec.isInsertMode())
                mVim.write(27);
            mVim.write(":w " + mLastDir + "/");
            return true;
        } else if (id == R.id.menu_toggle_soft_keyboard) {
            return true;
        } else if (id == R.id.menu_ESC) {
            mVim.showContent();
            return true;
        }
        return false;
    }

    public boolean onNavigationItemSelected(int pos, long id){
        showDirectory(mVim.getTabAdapter().getItem(pos).toString());
        return true;
    }

    private void showDirectory(String path) {
	    FileListFragment explorerFragment = FileListFragment.newInstance(path);
        explorerFragment.setFileChoosedListener(this);
        mVim.setSlidingMenuFragment((Fragment)explorerFragment);

        ArrayAdapter<CharSequence> adapter = mVim.getTabAdapter();

        adapter.clear();
        adapter.add(path);
        String curr = path;
        while (curr != null){
            File file = new File(curr);
            if(file.getParentFile() == null) break;
            curr = file.getParentFile().getAbsolutePath();
            adapter.add(curr);
        }
        adapter.notifyDataSetChanged();
        
        mVim.showTab(1);
        mLastDir = path;
    }

    public void onFileSelected(File file){
        if (file != null) {
			String path = file.getAbsolutePath();
            String ext = path.substring(path.lastIndexOf('.')+1);
			
			if (file.isDirectory()) {
                showDirectory(path);
            }else if (ext.equals("vrz")) {
                mLastDir = path;
                AlertDialog.Builder builder = new AlertDialog.Builder(mVim);
                builder.setMessage(R.string.restore_message)
                    .setTitle(R.string.restore_title)
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Intent intent = new Intent(mVim.getApplicationContext(), InstallProgress.class);
                            intent.setData(Uri.parse("file://"+mLastDir));
                            mVim.startActivityForResult(intent, VimTouch.REQUEST_VRZ);
                        }
                    });

                AlertDialog dialog = builder.create();
                dialog.show();

			} else {
                mVim.openNewFile(path);
                mVim.showContent();
			}
		} else {
			Toast.makeText(mVim, R.string.error_selecting_file, Toast.LENGTH_SHORT).show();
		}
    }
}
