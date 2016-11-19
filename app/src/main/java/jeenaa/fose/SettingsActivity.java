package jeenaa.fose;

import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.widget.FrameLayout;

import static android.view.View.generateViewId;

public class SettingsActivity extends AppCompatActivity
{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout frame = new FrameLayout(this);
        frame.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        if(Build.VERSION.SDK_INT > 16) frame.setId(generateViewId());
        else //noinspection ResourceType
            frame.setId(676442);

        setContentView(frame);

        getFragmentManager()
                .beginTransaction()
                .add(frame.getId(), new CreditsFragment())
                .commit();
    }

    public static class CreditsFragment extends PreferenceFragment
    {

        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.activity_settings);
        }


    }
}
