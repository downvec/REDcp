package mx.com.redcup.redcup;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import com.facebook.login.LoginManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import mx.com.redcup.redcup.myNavigationFragments.MapsFragment;
import mx.com.redcup.redcup.myNavigationFragments.NearbyFragment;
import mx.com.redcup.redcup.myNavigationFragments.UserSettingsFragment;
import mx.com.redcup.redcup.myNavigationFragments.UserProfileFragment;

public class NavActivity extends AppCompatActivity {
    private String TAG = "NavActivity (Main)";

    public String firebaseUID;
    Window window;

    //Create Fragment objects to be switched to
    MapsFragment mapFragment = new MapsFragment();
    NearbyFragment nearbyFragment = new NearbyFragment();
    UserProfileFragment profileFragment = new UserProfileFragment();
    UserSettingsFragment uSettingsFragment = new UserSettingsFragment();

    Fragment currentVisibleFragment;


    //Bottom nav, fragment switcher handler
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    switchToFragment(mapFragment);
                    return true;
                case R.id.navigation_dashboard:
                    switchToFragment(nearbyFragment);
                    return true;
                case R.id.navigation_user_profile:
                    switchToFragment(profileFragment);
                    return true;
                case R.id.navigation_settings:
                    switchToFragment(uSettingsFragment);
                    return true;
            }
            return false;
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nav);


        //Checking if the user is logged in. Else, send him to LoginScreen
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.e(TAG, "User is not logged in. Redirecting to LoginActivity");
            goLoginScreen();
        } else {
            firebaseUID = user.getUid();
        }




        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        //Get UI Elements

        //Getting Map initialized
        switchToFragment(mapFragment);
        currentVisibleFragment = mapFragment;

        String token = FirebaseInstanceId.getInstance().getToken();
        DatabaseReference mdatabase = FirebaseDatabase.getInstance().getReference().child("Users_parent").child(firebaseUID).child("device_token");
        mdatabase.setValue(token);

        window = getWindow();

    }



    //End of native and lifecycle methods. Begin created methods
    public void terminateAllSessions(View view) {
        Log.i(TAG,"User clicked logout bottom. Terminating Facebook and Firebase sessions");
        LoginManager.getInstance().logOut();
        FirebaseAuth.getInstance().signOut();
        goLoginScreen();
    }

    private void goLoginScreen() {
        Log.i(TAG,"User is not authenticated. Redirecting user to login screen");
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }



    public void switchToFragment(Fragment destination){
        /*
        FragmentManager manager = getFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        if (manager.findFragmentByTag(destination.getClass().getSimpleName()) == null){// This fragment not on manger
            transaction.add(R.id.Nav_activity_content,destination,destination.getClass().getSimpleName());
            transaction.commit();
        } else {// this means theres already this fragment on manger
            transaction.detach(currentVisibleFragment);
            transaction.attach(manager.findFragmentByTag(destination.getClass().getSimpleName()));
            transaction.commit();
        }

        currentVisibleFragment = destination;
        */
        getFragmentManager().beginTransaction().replace( R.id.Nav_activity_content, destination ).commit();
    }



}
