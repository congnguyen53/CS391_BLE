package com.example.cs391_ble

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.Thread.sleep


enum class PlayingState {
    PAUSED, PLAYING, STOPPED
}

private const val CLIENT_ID = "e0f6f52a8e2e4fd5a648e1ca573fae36"
private const val  REDIRECT_URI = "cs391://callback"

/**
 * Main activity in Android application
 * -Connects to spotify
 * -Will connect to BLE devices
 * =Will calculate packet success rate
 * -Will calculate sectors based on success rate...
 * ------------------------------------------------
 * - Might implement RSSI-based location and configure ToF...
 */
class MainActivity : AppCompatActivity() {
    /**
     * Creation of intent, set spotify button action to connect
     * to Spotify...
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /*
        When the button is clicked, change from disconnected to connected on screen and play
         default playlist.
         */
        SpotifyAPIBUTTON.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            SpotifyService.connect(this) {
                /*
                Testing purposes...  remove later on...
                 */
                SpotifyService.play("spotify:playlist:71JXQ7EwfZMKmLPrzKZAB4")
                // Toast message to confirm connection...
                Toast.makeText(this,"CONNECTED!",Toast.LENGTH_LONG).show()

            }
            sleep(1000)
            val intent = Intent(this, BLEConnect::class.java)
            startActivity(intent)
        }
    }

}

/**
 * Used for intent within manifest file...
 * Allows for new instance...
 */
class PlayerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}
/**
 * Connects to Spotify API by Intent to ask for permission inside spotify app.
 */
object SpotifyService {
    private var spotifyAppRemote: SpotifyAppRemote? = null
    private var connectionParams: ConnectionParams = ConnectionParams.Builder(CLIENT_ID)
        .setRedirectUri(REDIRECT_URI)
        .showAuthView(true)
        .build()

    fun connect(context: Context, handler: (connected: Boolean) -> Unit) {
        if (spotifyAppRemote?.isConnected == true) {
            handler(true)
            return
        }
        // Makes sure that spotify is either connected or disconnected
        val connectionListener = object : Connector.ConnectionListener {
            override fun onConnected(spotifyAppRemote: SpotifyAppRemote) {
                this@SpotifyService.spotifyAppRemote = spotifyAppRemote
                handler(true)
            }

            override fun onFailure(throwable: Throwable) {
                Log.e("SpotifyService", throwable.message, throwable)
                handler(false)
            }
        }
        SpotifyAppRemote.connect(context, connectionParams, connectionListener)
    }

    /*
     * Functions below are for performing tasks to play, pause, resume, etc...
     * Use functions for certain occasions...
     */
    fun play(uri: String) {
        spotifyAppRemote?.playerApi?.play(uri)
    }

    fun resume() {
        spotifyAppRemote?.playerApi?.resume()
    }

    fun pause() {
        spotifyAppRemote?.playerApi?.pause()
    }

    /*
    Change playing stat within application...
     */
    fun playingState(handler: (PlayingState) -> Unit) {
        spotifyAppRemote?.playerApi?.playerState?.setResultCallback { result ->
            if (result.track.uri == null) {
                handler(PlayingState.STOPPED)
            } else if (result.isPaused) {
                handler(PlayingState.PAUSED)
            } else {
                handler(PlayingState.PLAYING)
            }
        }
    }
}

