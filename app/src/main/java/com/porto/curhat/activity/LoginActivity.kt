package com.porto.curhat.activity

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.facebook.login.widget.LoginButton
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.signin.SignIn
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.porto.curhat.R
import kotlinx.android.synthetic.main.reset_pass_dialog.view.*

class LoginActivity : AppCompatActivity(), View.OnClickListener {

    private val TAG = "Login"
    private var RC_FACEBOOK_SIGN_IN: Int = 64206
    private var RC_GOOGLE_SIGN_IN: Int = 9001
    private lateinit var mAuth:FirebaseAuth
    private lateinit var mCallbackManager: CallbackManager

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnSignIn: Button
    private lateinit var btnGoogleLogin: SignInButton
    private lateinit var btnFacebookLogin: LoginButton

    private lateinit var mGoogleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        //Views
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnSignIn = findViewById(R.id.btnSignIn)
        btnGoogleLogin = findViewById(R.id.sign_in_button)
        btnFacebookLogin = findViewById(R.id.button_facebook_login)


        btnSignIn.setOnClickListener(this)
        btnGoogleLogin.setOnClickListener(this)
        btnFacebookLogin.setOnClickListener(this)

        //firebase config
        FirebaseApp.initializeApp(applicationContext)
        mAuth = FirebaseAuth.getInstance()

        //Google sign in config
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        //Facebook sign in config
        mCallbackManager = CallbackManager.Factory.create()
        btnFacebookLogin.setReadPermissions("email", "public_profile")
        btnFacebookLogin.registerCallback(mCallbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(loginResult: LoginResult) {
                Log.d(TAG, "facebook:onSuccess:$loginResult")
                handleFacebookAccessToken(loginResult.accessToken)
            }

            override fun onCancel() {
                Log.d(TAG, "facebook:onCancel")
            }

            override fun onError(error: FacebookException) {
                Log.d(TAG, "facebook:onError", error)
            }
        })


    }

    override fun onStart() {
        super.onStart()
        val currentUser = mAuth.currentUser
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_GOOGLE_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e)

            }

        } else if(requestCode == RC_FACEBOOK_SIGN_IN){
            mCallbackManager.onActivityResult(requestCode, resultCode, data)
        }

    }

    private fun handleFacebookAccessToken(token: AccessToken) {
        Log.d(TAG, "handleFacebookAccessToken:$token")

        val credential = FacebookAuthProvider.getCredential(token.token)
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "signInWithCredential:success")
                    } else {
                        Log.w(TAG, "signInWithCredential:failure", task.exception)
                        Toast.makeText(this@LoginActivity, "Authentication failed.",
                                Toast.LENGTH_SHORT).show()
                    }
                }
    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.id!!)
        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "signInWithCredential:success")
                        val user = mAuth.currentUser
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "signInWithCredential:failure", task.exception)
                        Snackbar.make(findViewById(R.id.main_layout), "Authentication Failed.", Snackbar.LENGTH_SHORT).show()
                    }
                }
    }

    fun signInGoogle() {
        val signInIntent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN)
    }
    fun signInEmail(email: String, password: String) {
        Log.d(TAG, "signIn:" + email);
        if (!validateForm()) {
            return;
        }

        // [START sign_in_with_email]
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) {task ->
                    if(task.isSuccessful){
                        Log.d(TAG, "signInWithEmail:success")
                        val user = mAuth.currentUser
                    } else {
                        Log.w(TAG, "signInWithEmail:Failed.", task.exception)
                        Snackbar.make(findViewById(R.id.main_layout), "Sign In Failed.", Snackbar.LENGTH_SHORT).show()
                    }
                }
    }

     private fun validateForm(): Boolean {
         var valid: Boolean = true

         var email: String = etEmail.text.toString()
         if(TextUtils.isEmpty(email)){
             etEmail.setError("Required.")
             valid = false
         } else {
             etEmail.setError(null)
         }

         var password: String = etPassword.text.toString()
         if (TextUtils.isEmpty(password)){
             etPassword.setError("Required")
             valid = false
         } else {
             etPassword.setError(null)
         }
         return valid
    }

    private fun sendpasswordReset(email: String){
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener {
                    if (it.isSuccessful){
                        Toast.makeText(applicationContext, "Email sent.", Toast.LENGTH_SHORT).show()
                    }
                }
    }

    fun signOut(){
        mAuth.signOut()
        LoginManager.getInstance().logOut()
    }

    override fun onClick(v: View) {
        val i = v.id
        if (i == R.id.sign_in_button) {
            signInGoogle()
        } else if (i == R.id.btnSignIn) {
            signInEmail(etEmail.text.toString(), etPassword.text.toString())
        } else if (i == R.id.tvForgetPassword) {
            val mDialogView = LayoutInflater.from(this).inflate(R.layout.reset_pass_dialog, null)
            val mBuilder = AlertDialog.Builder(this)
                    .setView(mDialogView)
                    .setTitle("Reset Password")
            val mAlertDialog = mBuilder.show()
            mDialogView.btnResetPass.setOnClickListener{
                mAlertDialog.dismiss()
                val email = mDialogView.etEmail.text.toString()
                sendpasswordReset(email)
            }
        }
    }

}
