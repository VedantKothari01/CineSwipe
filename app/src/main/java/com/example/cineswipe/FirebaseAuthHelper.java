package com.example.cineswipe;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class FirebaseAuthHelper {
    private FirebaseAuth mAuth;

    public FirebaseAuthHelper() {
        mAuth = FirebaseAuth.getInstance();
    }

    public void registerUser(String email, String password, OnAuthCompleteListener listener) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        listener.onSuccess(mAuth.getCurrentUser());
                    } else {
                        listener.onFailure(task.getException());
                    }
                });
    }

    public void loginUser(String email, String password, OnAuthCompleteListener listener) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        listener.onSuccess(mAuth.getCurrentUser());
                    } else {
                        listener.onFailure(task.getException());
                    }
                });
    }

    public interface OnAuthCompleteListener {
        void onSuccess(FirebaseUser user);
        void onFailure(Exception e);
    }

    public FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }
}
