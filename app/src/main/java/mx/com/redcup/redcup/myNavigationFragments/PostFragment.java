package mx.com.redcup.redcup.myNavigationFragments;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Fragment;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.StringSignature;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;
import mx.com.redcup.redcup.R;
import mx.com.redcup.redcup.myDataModels.MyPosts;

import static android.app.Activity.RESULT_OK;


public class PostFragment extends Fragment {

    private final Runnable updateFriendsFeedList = new Runnable() {
        @Override
        public void run() {
            DatabaseReference friendsRef = mDatabase.child("Users_parent").child(getCurrentFirebaseUID()).child("userFriends");
            friendsRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for (DataSnapshot snapshot: dataSnapshot.getChildren()){
                        String friend = snapshot.getValue(String.class);
                        DatabaseReference friendReference = mDatabase.child("Feeds").child(friend);
                        friendReference.updateChildren(createdPost);
                    }
                }
                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });

        }
    };

    private final Runnable revealAnimationRunnable = new Runnable() {
        @Override
        public void run() {

            cx = (myContainer.getRight() -10);
            cy = (myContainer.getBottom() - 10);
            finalRadius = Math.max(myContainer.getWidth(), myContainer.getHeight());

            Animator anim = ViewAnimationUtils.createCircularReveal(myContainer, cx, cy, 0, finalRadius);

            myContainer.setVisibility(View.VISIBLE);
            anim.start();

            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    window.setStatusBarColor(getResources().getColor(R.color.colorAccentDark));
                    myContainer.setBackgroundColor(getResources().getColor(R.color.white));
                }
            });
        }
    };


    RelativeLayout myContainer;
    Button sendPost;
    EditText postContent;
    Button openCamera;
    Button openGallery;
    ImageView postImage;

    Map<String,Object> createdPost;
    String createdPostID;

    int cx;
    int cy;
    int finalRadius;

    Window window;
    DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
    DatabaseReference mDatabaseUsers = FirebaseDatabase.getInstance().getReference().child("Users_parent");
    DatabaseReference mFeedReference = FirebaseDatabase.getInstance().getReference().child("Feeds");
    StorageReference mStorage = FirebaseStorage.getInstance().getReference();


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable final ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_newpost_screen, container, false);

        window = getActivity().getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        myContainer = (RelativeLayout) view.findViewById(R.id.container_newpost);
        sendPost = (Button) view.findViewById(R.id.btn_newpost_send);
        postContent = (EditText) view.findViewById(R.id.et_newpost_postcontent);
        openCamera = (Button) view.findViewById(R.id.btn_prompt_camera);
        openGallery = (Button) view.findViewById(R.id.btn_prompt_gallery);
        postImage = (ImageView) view.findViewById(R.id.iv_eventpic_postscreen);

        myContainer.post(revealAnimationRunnable);

        createdPostID = mDatabase.push().getKey();

        sendPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String content = postContent.getText().toString();

                createPost(content,createdPostID, getCurrentFirebaseUID());
            }
        });

        openCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(takePictureIntent, 2);
            }
        });

        openGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED ) {

                    ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                    return;
                }
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent,1);
            }
        });

        return view;
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        window.setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));
    }

    public void createPost(String content, String pushID, String authorUID){
        Log.e("alksdfjla;s","IS it working");
        MyPosts newPost = new MyPosts(content,authorUID,pushID);

        mDatabase.child("Events_parent").child(pushID).setValue(newPost);

        //Update userPosts and feed list;
        Map<String,Object> postAsList = new HashMap<>();
        postAsList.put(pushID,pushID);

        createdPost = postAsList;

        mDatabaseUsers.child(getCurrentFirebaseUID()).child("user_posts").updateChildren(postAsList);
        mFeedReference.child(getCurrentFirebaseUID()).updateChildren(postAsList);
        updateFriendsFeedList.run();

        getFragmentManager().popBackStack();
        Toast.makeText(getActivity(),"Post created!",Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent returnedImage){
        if (resultCode == RESULT_OK){
            StorageMetadata metadata = new StorageMetadata.Builder().setContentType("image/png").setContentEncoding("png").build();
            Uri imageUri = returnedImage.getData();
            mStorage.child(createdPostID).child(createdPostID).putFile(imageUri,metadata);
            Glide.with(getActivity()).using(new FirebaseImageLoader()).load(mStorage.child(createdPostID).child(createdPostID))
                    .signature(new StringSignature(createdPostID)).into(postImage);
        }
    }

    public String getCurrentFirebaseUID(){
        String UID = "";
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null){
            UID = user.getUid();
        } else {
            Log.e("TAG","User is unexpectedly null.");
        }
        return UID;
    }

}