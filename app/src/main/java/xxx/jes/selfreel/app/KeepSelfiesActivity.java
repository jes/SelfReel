package xxx.jes.selfreel.app;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class KeepSelfiesActivity extends Activity {
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;
    private List<String> filenames;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_keep_selfies);

        getActionBar().setTitle("SelfReel");
        getActionBar().setSubtitle("Keep some selfies");

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        filenames = getIntent().getExtras().getStringArrayList("filenames");
        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager(), filenames);

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOffscreenPageLimit(3);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_keep_selfies, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_share:
                shareSelfie();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void shareSelfie() {
        try {
            String photoUri = MediaStore.Images.Media.insertImage(
                    getContentResolver(), filenames.get(mViewPager.getCurrentItem()), null, null);

            Uri uri = Uri.parse(photoUri);
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("image/jpeg");
            share.putExtra(Intent.EXTRA_STREAM, uri);
            startActivity(Intent.createChooser(share, "Share Image"));
        } catch (Exception e) {
            Log.d("Foo", "Shit happened.");
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        private List<String> filenames;

        public SectionsPagerAdapter(FragmentManager fm, List<String> filenames) {
            super(fm);
            this.filenames = filenames;
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return new SelfieFragment(filenames.get(position));
        }

        @Override
        public int getCount() {
            return filenames.size();
        }
    }

    public static class SelfieFragment extends Fragment {
        Drawable drawable;
        String filename;

        public SelfieFragment(String filename) {
            this.filename = filename;
            this.drawable = Drawable.createFromPath(filename);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_keep_selfie, container, false);
            ImageView img = (ImageView) rootView.findViewById(R.id.imageView);
            img.setImageDrawable(drawable);
            return rootView;
        }
    }
}
