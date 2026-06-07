package com.example.pixtagarc.controller;

import com.example.pixtagarc.config.DatabaseConfig;
import com.example.pixtagarc.domain.Tag;
import com.example.pixtagarc.repository.ImageTagRepository;
import com.example.pixtagarc.repository.TagRepository;
import com.example.pixtagarc.service.TagService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * タグ編集ダイアログのコントローラークラス。
 *
 * <p>画像に対してタグを追加・削除するモーダルダイアログを制御する。
 * 現在のタグ一覧の表示、新規タグの追加、既存タグからの選択、タグの削除を担当する。
 *
 * @author pixtagarc
 * @since 1.0.0
 */
public class TagEditController implements Initializable {

    /** ロガー。 */
    private static final Logger log = LoggerFactory.getLogger(TagEditController.class);

    /** 現在のタグListView。 */
    @FXML private ListView<String> currentTagsListView;

    /** タグ削除ボタン。 */
    @FXML private Button removeTagButton;

    /** 新規タグ入力フィールド。 */
    @FXML private TextField newTagField;

    /** タグ追加ボタン。 */
    @FXML private Button addTagButton;

    /** 既存タグ選択ComboBox。 */
    @FXML private ComboBox<String> existingTagCombo;

    /** 閉じるボタン。 */
    @FXML private Button closeButton;

    /** タグサービス。 */
    private TagService tagService;

    /** 編集対象の画像ID。 */
    private Long imageId;

    /** 現在の画像に付与されているタグのリスト。 */
    private List<Tag> currentTags;

    /**
     * コントローラーを初期化する。
     *
     * @param location  FXMLのURL
     * @param resources リソースバンドル
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.debug("TagEditControllerを初期化します");
        DatabaseConfig dbConfig = DatabaseConfig.getInstance();
        TagRepository tagRepository = new TagRepository(dbConfig);
        ImageTagRepository imageTagRepository = new ImageTagRepository(dbConfig);
        tagService = new TagService(tagRepository, imageTagRepository);
    }

    /**
     * 編集対象の画像IDを設定し、タグ情報を読み込む。
     *
     * @param imageId 編集対象の画像ID
     */
    public void setImageId(Long imageId) {
        this.imageId = imageId;
        loadCurrentTags();
        loadExistingTags();
    }

    /**
     * 現在の画像に付与されているタグを読み込む。
     */
    private void loadCurrentTags() {
        try {
            currentTags = tagService.getTagsForImage(imageId);
            List<String> tagNames = currentTags.stream()
                    .map(Tag::getName)
                    .collect(Collectors.toList());
            currentTagsListView.setItems(FXCollections.observableArrayList(tagNames));
        } catch (Exception e) {
            log.error("タグの読み込みに失敗しました: imageId={}", imageId, e);
        }
    }

    /**
     * 全既存タグをComboBoxに読み込む。
     */
    private void loadExistingTags() {
        try {
            List<Tag> allTags = tagService.findAll();
            List<String> tagNames = allTags.stream()
                    .map(Tag::getName)
                    .collect(Collectors.toList());
            existingTagCombo.setItems(FXCollections.observableArrayList(tagNames));
        } catch (Exception e) {
            log.error("既存タグの読み込みに失敗しました", e);
        }
    }

    /**
     * 新規タグを追加する。
     */
    @FXML
    private void onAddTag() {
        String tagName = newTagField.getText();
        if (tagName == null || tagName.trim().isEmpty()) {
            return;
        }
        addTagByName(tagName.trim());
        newTagField.clear();
    }

    /**
     * 既存タグから選択して追加する。
     */
    @FXML
    private void onSelectExistingTag() {
        String selected = existingTagCombo.getValue();
        if (selected != null && !selected.isEmpty()) {
            addTagByName(selected);
        }
    }

    /**
     * タグ名で画像にタグを追加する。
     *
     * @param tagName タグ名
     */
    private void addTagByName(String tagName) {
        try {
            Tag tag = tagService.createOrGet(tagName);
            // 既に付与されているタグは追加しない
            boolean alreadyExists = currentTags.stream()
                    .anyMatch(t -> t.getId().equals(tag.getId()));
            if (!alreadyExists) {
                tagService.addTagToImage(imageId, tag.getId());
                loadCurrentTags();
                updateFtsIndex();
                log.info("タグを追加しました: imageId={}, tagName={}", imageId, tagName);
            }
        } catch (Exception e) {
            log.error("タグの追加に失敗しました: tagName={}", tagName, e);
            showError("タグの追加に失敗しました", e.getMessage());
        }
    }

    /**
     * 選択されたタグを削除する。
     */
    @FXML
    private void onRemoveTag() {
        int selectedIndex = currentTagsListView.getSelectionModel().getSelectedIndex();
        if (selectedIndex < 0 || selectedIndex >= currentTags.size()) {
            return;
        }
        Tag tagToRemove = currentTags.get(selectedIndex);
        try {
            tagService.removeTagFromImage(imageId, tagToRemove.getId());
            loadCurrentTags();
            updateFtsIndex();
            log.info("タグを削除しました: imageId={}, tagId={}", imageId, tagToRemove.getId());
        } catch (Exception e) {
            log.error("タグの削除に失敗しました: tagId={}", tagToRemove.getId(), e);
            showError("タグの削除に失敗しました", e.getMessage());
        }
    }

    /**
     * 画像のFTS5インデックスを再構築する。
     *
     * <p>タグ追加・削除後にキーワード検索でタグ名がヒットするようにFTS5を更新する。
     */
    private void updateFtsIndex() {
        try {
            DatabaseConfig dbConfig = DatabaseConfig.getInstance();
            com.example.pixtagarc.repository.ImageRepository imageRepository =
                    new com.example.pixtagarc.repository.ImageRepository(dbConfig);
            com.example.pixtagarc.repository.ImageTagRepository imageTagRepository =
                    new com.example.pixtagarc.repository.ImageTagRepository(dbConfig);

            com.example.pixtagarc.domain.Image image = imageRepository.findById(imageId).orElse(null);
            if (image == null) return;

            String tagsText = imageTagRepository.getTagsTextForImage(imageId);
            String authorName = "";
            if (image.getAuthorId() != null) {
                com.example.pixtagarc.repository.AuthorRepository authorRepo =
                        new com.example.pixtagarc.repository.AuthorRepository(dbConfig);
                authorName = authorRepo.findById(image.getAuthorId())
                        .map(a -> a.getName()).orElse("");
            }
            imageRepository.insertFts(imageId, image.getFileName(), tagsText, authorName);
        } catch (Exception e) {
            log.warn("FTS5インデックスの更新に失敗しました（キーワード検索に影響する可能性）: imageId={}", imageId, e);
        }
    }

    /**
     * ダイアログを閉じる。
     */
    @FXML
    private void onClose() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    /**
     * エラーダイアログを表示する。
     *
     * @param title   タイトル
     * @param message メッセージ
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
