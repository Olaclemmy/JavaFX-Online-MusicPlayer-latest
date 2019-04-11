package utils;

import controller.BottomController;
import controller.LyricController;
import entity.KuGouMusicPlay;
import flag.MusicResources;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import listener.MyPlayerChangeListener;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static flag.CommonResources.IMAGE_PAUSE;
import static flag.CommonResources.IMAGE_PLAYING;

/**
 * Media类是final类型的，不能继承
 */
public class MusicUtils implements IMusic {

    //
    private static MediaPlayer mediaPlayer = null;
    //是否Player初始化了，被new了
    private static final SimpleBooleanProperty IS_PLAYER_INIT = new SimpleBooleanProperty(false);
    //
    private static Button btnPlay = null;
    //记录当前的音量
    private double currVolume = 0.3;
    //当前的播放状态，解决缓冲完成，自动播放的问题，因为用户可能点击了pause
    private  boolean isUserSelectPauseStatus = false;

    /* 单例 */
    private static MusicUtils instance = null;
    private MusicUtils() {
        /*只要music 的内容改变，就调用这个，想法：只要URL改变，说明来了新音乐，播放它 */

        /*发现弊端：网速搅蛮的情况下，会发生阻塞问题*/
        /*更新一下：阻塞问题，不是下面的代码引起的，现在怀疑是mediaplayer的Status*/
        /*MUSIC_URL.addListener((observable, oldValue, newValue) -> {
            System.out.println("url change");

            *//*if (musicService.isRunning()) musicService.cancel();
            musicService.start();
            musicService.setOnSucceeded(event -> {
                mediaPlayer = (MediaPlayer) event.getSource().getValue();
                onSucc();
            });*//*

            //20190123-先判断MediaPlayer是否为空，不为空的话，先释放掉之前的媒体资源，避免多重播放
            if (mediaPlayer != null) {
                mediaPlayer.dispose();
                mediaPlayer.bufferProgressTimeProperty().removeListener(bufferListener);
            }
            //20190123
            mediaPlayer = new MediaPlayer(new Media(MUSIC_URL.get()));
            //
            mediaPlayer.setAutoPlay(true);
            //
            mediaPlayer.statusProperty().removeListener(statusChangeListener);
            //
            mediaPlayer.bufferProgressTimeProperty().addListener(bufferListener);
        });*/
    }

    public void playNewSong(String url) {
        System.out.println("url change");
        //20190123-先判断MediaPlayer是否为空，不为空的话，先释放掉之前的媒体资源，避免多重播放
        if (mediaPlayer != null) {
            mediaPlayer.dispose();
            mediaPlayer.bufferProgressTimeProperty().removeListener(bufferListener);
        }
        mediaPlayer = new MediaPlayer(new Media(url));
        mediaPlayer.setAutoPlay(true);
        mediaPlayer.setOnStalled(() -> System.out.println("mediaPlayer.setOnStalled"));
        mediaPlayer.bufferProgressTimeProperty().addListener(bufferListener);
        this.startListener();
        mediaPlayer.setOnReady(() -> {
            System.out.println("mediaPlayer.setOnReady");
        });
    }

    //单例模式
    public static MusicUtils getInstance() {
        if (instance == null) {
            synchronized (MusicResources.class) {
                if (instance == null) instance = new MusicUtils();
            }
        }
        return instance;
    }//getInstance

    private ChangeListener<Duration> bufferListener = (observable, oldValue, newValue) -> {
        System.out.print(newValue + " - ");
        if (newValue.toMillis() >= mediaPlayer.getStopTime().toMillis()) {
            System.out.println("缓冲完成");
            if (!isUserSelectPauseStatus) mediaPlayer.play();
            ((BottomController)BottomController.BC_CONTEXT.get(BottomController.class.getName())).showBufferingText(false, null);
            ((BottomController)BottomController.BC_CONTEXT.get(BottomController.class.getName())).showBufferingNetworkSlowText(false);
        } else if (newValue.toMillis() >= 5000.0 + mediaPlayer.getCurrentTime().toMillis()) {
//            System.out.println("缓冲超过5秒，可以播放了。。");
            if (!isUserSelectPauseStatus) mediaPlayer.play();
            ((BottomController)BottomController.BC_CONTEXT.get(BottomController.class.getName())).showBufferingText(true, "正在缓冲");
            ((BottomController)BottomController.BC_CONTEXT.get(BottomController.class.getName())).showBufferingNetworkSlowText(false);
        } else if (newValue.toMillis() - mediaPlayer.getCurrentTime().toMillis() < 5000){
            System.out.println("缓冲小于5秒，缓冲较慢。。。");
            mediaPlayer.pause();
            ((BottomController)BottomController.BC_CONTEXT.get(BottomController.class.getName())).showBufferingText(true, "缓冲小于5秒，缓冲较慢");
            ((BottomController)BottomController.BC_CONTEXT.get(BottomController.class.getName())).showBufferingNetworkSlowText(true);
        } else {
            System.out.println("...................啦啦啦");
            if (!isUserSelectPauseStatus) mediaPlayer.play();
            ((BottomController)BottomController.BC_CONTEXT.get(BottomController.class.getName())).showBufferingText(false, null);
            ((BottomController)BottomController.BC_CONTEXT.get(BottomController.class.getName())).showBufferingNetworkSlowText(false);
        }
    };

    @Override
    public void play() {
        System.out.println("playing method begin");
        if (mediaPlayer.getMedia() == null) {
            System.out.println(mediaPlayer.getMedia());
            return;
        } else {
            System.out.println("else--media != null: " + mediaPlayer.getMedia().getSource());
            switch (mediaPlayer.getStatus()) {
                case PLAYING:
                    mediaPlayer.pause();
                    isUserSelectPauseStatus = true;
                    System.out.println("playing-->pause");
                    break;
                case PAUSED:
                    mediaPlayer.setVolume(currVolume);
                    mediaPlayer.play();
                    isUserSelectPauseStatus = false;
                    System.out.println("pause-->playing");
                    break;
                case STOPPED:
                    System.out.println("stop-->playing");
                    mediaPlayer.setVolume(currVolume);
                    mediaPlayer.play();
                    isUserSelectPauseStatus = false;
                    //从头开始播放
                    mediaPlayer.seek(Duration.millis(0.0));
                    break;
                case READY:
                    System.out.println("ready-->playing");
                    mediaPlayer.setVolume(currVolume);
                    mediaPlayer.play();
                    isUserSelectPauseStatus = false;
                    break;
                case UNKNOWN:
                    System.out.println("UNKNOWN-->playing");
                    mediaPlayer.setVolume(currVolume);
                    mediaPlayer.play();
                    isUserSelectPauseStatus = false;
                    break;
            }
        }
        System.out.println("playing method end");
    }//

    @Override
    public void pause() {
        if (mediaPlayer.getMedia() != null)
            mediaPlayer.pause();
    }

    @Override
    public void stop() {
        if (mediaPlayer.getMedia() != null)
            mediaPlayer.stop();
    }

    @Override
    public void seek(Duration duration) {
        if (mediaPlayer.getMedia() != null)
            mediaPlayer.seek(duration);
    }

    @Override
    public boolean isPlayerNull() {
        return mediaPlayer == null;
    }

    @Override
    public MediaPlayer.Status getCurrStatus() {
        if (mediaPlayer != null) return mediaPlayer.getStatus();
        else return null;
    }

    @Override
    public void setVolume(double volume) {
        if (mediaPlayer == null) return;
        if (volume >= 1.0) {
            mediaPlayer.setVolume(1.0);
            currVolume = 1.0;
        } else if (volume <= 0.0) {
            mediaPlayer.setVolume(0.0);
            currVolume = 0.0;
        } else {
            mediaPlayer.setVolume(volume);
            currVolume = volume;
        }
    }

    private MyPlayerChangeListener myPlayerChangeListener = new MyPlayerChangeListener();

    @Override
    public void setListener(PlayerListenerCallback callback, LyricController.SyncLyricCallback syncLyricCallback) {
        myPlayerChangeListener.setCallback(callback);
        myPlayerChangeListener.setSyncLyricCallback(syncLyricCallback);
    }//

    @Override
    public void startListener() {
        myPlayerChangeListener.setMediaPlayer(mediaPlayer);
        mediaPlayer.currentTimeProperty().addListener(myPlayerChangeListener);
        mediaPlayer.statusProperty().addListener(statusChangeListener);
    }

    @Override
    public void removeListener() {
        mediaPlayer.currentTimeProperty().removeListener(myPlayerChangeListener);
        mediaPlayer.statusProperty().removeListener(statusChangeListener);
    }//

    @Override
    public void initMusicInfo(KuGouMusicPlay.DataBean selectItem, PlayerCallback callback) {
        callback.initViewOfMusicInfo(selectItem);
    }

    private ChangeListener statusChangeListener = (observable, oldValue, newValue) -> {
        if (mediaPlayer == null) return;

        switch (mediaPlayer.getStatus()) {
            case PLAYING:
                Platform.runLater(() -> btnPlay.setGraphic(IMAGE_PLAYING));
                break;
            default:
                Platform.runLater(() -> btnPlay.setGraphic(IMAGE_PAUSE));
                break;
        }
    };

    @Override
    public void listenPlayerStatus(Button btn) {
        btnPlay = btn;
    }
}