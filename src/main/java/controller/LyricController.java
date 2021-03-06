package controller;

import animatefx.animation.BounceInRight;
import animatefx.animation.BounceOutRight;
import base.BaseController;
import com.jfoenix.controls.JFXButton;
import entity.KuGouMusicPlay;
import entity.Lyric;
import flag.Flags;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.util.Callback;
import javafx.util.Duration;
import utils.LoadUtil;

import java.math.BigDecimal;
import java.net.URL;
import java.util.*;

import static flag.Flags.IS_LYRIC_VISIBLE;

/**
 * Author QAQCoder , Email:QAQCoder@qq.com
 * Create time 2019/5/30 12:04
 * Class description：
 */
public class LyricController extends BaseController implements Initializable {

    @FXML public Button btnBackG;
    @FXML public ListView<Lyric> listViewLyric;
    @FXML public AnchorPane rootPane;
    public JFXButton btnSetLyricTime;

    private double currPosition = 0.8;

    private Timeline timeline = null;

    private ObservableList<Lyric> lyricObservableList = null;
    private List<Lyric> lyricList = new ArrayList<>();
    private TreeMap<Long, Integer> lyricMap = new TreeMap<>();
    private SimpleIntegerProperty delayTimeProperty = new SimpleIntegerProperty(1000);
    //
    private KuGouMusicPlay.DataBean dataBean = null;
    private MainViewController mvc = (MainViewController) BC_CONTEXT.get(MainViewController.class.getName());

    @Override
    public void initialize(URL location, ResourceBundle resources) {
//        System.out.println("---LyricController-----initialize---");
        BC_CONTEXT.put(LyricController.class.getName(), this);
        initView();
        initEvent();
    }

    private void initEvent() {
        btnSetLyricTime.setOnMouseClicked(me -> {
            TextField tf = new TextField("1000");
            tf.setPrefWidth(50);
            tf.textProperty().addListener((observable, oldValue, newValue) -> delayTimeProperty.set(Integer.parseInt(newValue)));

            MenuItem item1 = new MenuItem("歌词调整", tf);
            MenuItem item2 = new MenuItem("歌词铺满");
            ContextMenu menu = new ContextMenu(item1, item2);
            item2.setOnAction(ae ->{
                if (mvc.centerPaneList.size() == 2) {
                    BigDecimal decimal = new BigDecimal(mvc.splitPane.getDividerPositions()[0]);

                    switch (decimal.compareTo(new BigDecimal(0.05))) {
                        case -1:
                            System.out.println(Arrays.toString(mvc.splitPane.getDividerPositions()));
                            mvc.splitPane.setDividerPositions(Flags.LYRIC_PANE_DIVISION);
                            break;
                        case 1:
                            System.out.println(Arrays.toString(mvc.splitPane.getDividerPositions()));
                            mvc.splitPane.setDividerPositions(0.0);
                            break;
                    }
                }
            });
            menu.show(btnSetLyricTime.getScene().getWindow(), me.getScreenX()-100, me.getScreenY()+25);
        });
    }//init event

    private void initView() {
        System.out.println("LyricController-->initView()");
         //设置歌词ListView的cell填充方式
        listViewLyric.setCellFactory(new Callback<ListView<Lyric>, ListCell<Lyric>>() {
            @Override
            public ListCell<Lyric> call(ListView<Lyric> param) {
                return new ListCell<Lyric>(){
                    @Override
                    protected void updateItem(Lyric item, boolean empty) {
                        super.updateItem(item, empty);
                        if (null != item) setText(item.getText());
                    }
                };
            }
        });
        lyricObservableList = listViewLyric.getItems();
    }// initView over

    /**
     * 更新歌词
     * @param data 这里data没用
     * @param flag
     */
    @Override
    public void updateUi(Object data, int flag) {
        System.out.println("-----LyricController--->updateUi---");

        if (flag == 0 && !lyricObservableList.isEmpty()) {
            if (lyricList.isEmpty()) return;    //没有歌词，直接return
            Lyric lyric = lyricList.get(0);
            Lyric lyric1 = lyricObservableList.get(0);
            //double的比较方法
            int compare = lyric.getTime().compareTo(lyric1.getTime());
            if (compare != 0 && !lyric.getText().equals(lyric1.getText())) {
                clearLyric();
            }
        } else {
            clearLyric();
        }
    }//updateUi

    private void clearLyric() {
        if (Platform.isFxApplicationThread()) {
            if (!lyricObservableList.isEmpty()) lyricObservableList.clear();
            lyricObservableList.addAll(lyricList);
            btnBackG.setText(dataBean.getAudio_name());
            listViewLyric.refresh();
        } else {
            Platform.runLater(() -> {
                if (!lyricObservableList.isEmpty()) lyricObservableList.clear();
                lyricObservableList.addAll(lyricList);
                btnBackG.setText(dataBean.getAudio_name());
                listViewLyric.refresh();
            });
        }
    }

    @Override
    public void initData(Object data) {
        System.out.println("LyricController---initData---");
        Map<String, Object> objectMap = (Map<String, Object>) data;
        //取出歌手名字和歌曲名称
        dataBean = (KuGouMusicPlay.DataBean) objectMap.get("musicInfo");
        System.out.println("歌曲名称：" + dataBean.getAudio_name());
        //取出歌词
        Map<String, Object> dataMap = (Map<String, Object>) objectMap.get("musicLyric");
        if (!lyricMap.isEmpty()) lyricMap.clear();
        //歌词索引
        this.lyricMap = (TreeMap<Long, Integer>) dataMap.get("map_index");
        //歌词索引对应的内容
        this.lyricList = (List<Lyric>) dataMap.get("lyric_list");
        updateUi(null, 1);
    }//

    //最终版本：歌词回调显示，时间容错已达到最优，我可真是个小机灵鬼，嘻嘻嘻（自娱自乐）
    public SyncLyricCallback callback = (Duration duration) -> {
        if (listViewLyric == null || !IS_LYRIC_VISIBLE.get()) {
            return;
        }
        long time = Long.parseLong(String.format("%.0f", duration.toSeconds()));
        if (lyricMap.containsKey(time)) {
            Integer index = lyricMap.get(time);
            updateGui(index);
        }
    };//

    //通过动画效果达到微调歌词显示的效果，歌词显示默认延迟一秒，我可真是个小机灵鬼，嘻嘻嘻
    private void updateGui(int index) {
        if (timeline == null) timeline = new Timeline();
        KeyFrame kf2 = new KeyFrame(Duration.millis(delayTimeProperty.get()), e -> {
            listViewLyric.getSelectionModel().select(index);
            //往回缩一点，嘿嘿嘿，我可真是个小机灵鬼~~~QWQ
            listViewLyric.scrollTo(index >= 5 ? index-5: 0);
        });
        timeline.getKeyFrames().addAll(new KeyFrame(Duration.millis(0)), kf2);
        Platform.runLater(timeline::play);
    }

    public interface SyncLyricCallback { void syncLyric(Duration duration);}
    //end 20190122-加入歌词回调显示

    /**
     * 显示歌词面板方法
     */
    public void showLyric() {
        if (Flags.lyricPane == null) Flags.lyricPane = LoadUtil.loadFXML("fxml/items/lyric_view.fxml");

        if (IS_LYRIC_VISIBLE.get()) {
            new BounceOutRight(Flags.lyricPane).setResetOnFinished(true).play();
            currPosition = mvc.splitPane.getDividerPositions()[0];
            Flags.LYRIC_PANE_DIVISION = currPosition;
            mvc.centerPaneList.remove(Flags.lyricPane);
            IS_LYRIC_VISIBLE.setValue(false);
        } else {
            mvc.centerPaneList.add(Flags.lyricPane);
            new BounceInRight(Flags.lyricPane).setResetOnFinished(true).play();
            mvc.splitPane.setDividerPositions(currPosition);
            IS_LYRIC_VISIBLE.setValue(true);
            updateUi(null, 0);
        }
    }//
}
