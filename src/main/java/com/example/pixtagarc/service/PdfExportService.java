package com.example.pixtagarc.service;

import com.example.pixtagarc.domain.Image;
import com.example.pixtagarc.dto.PdfExportRequest;
import com.example.pixtagarc.exception.ImageNotFoundException;
import com.example.pixtagarc.exception.PdfExportException;
import com.example.pixtagarc.repository.ImageRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * PDF出力サービスクラス。
 *
 * <p>Apache PDFBoxを使用して、指定された画像をPDFファイルに出力する。
 * 1ページあたりの枚数・用紙サイズを設定可能。
 *
 * @author pixtagarc
 * @since 1.0.0
 */
public class PdfExportService {

    /** ロガー。 */
    private static final Logger log = LoggerFactory.getLogger(PdfExportService.class);

    /** 画像リポジトリ。 */
    private final ImageRepository imageRepository;

    /**
     * コンストラクタ。
     *
     * @param imageRepository 画像リポジトリ
     */
    public PdfExportService(ImageRepository imageRepository) {
        this.imageRepository = imageRepository;
    }

    /**
     * PDF出力を実行する。
     *
     * <p>出力フロー:
     * <ol>
     *   <li>PDFBoxで新規ドキュメント作成</li>
     *   <li>画像を1枚ずつページに配置（設定に応じてN枚/ページ）</li>
     *   <li>ファイル保存</li>
     * </ol>
     *
     * @param request PDF出力リクエスト
     * @throws PdfExportException PDF出力に失敗した場合
     */
    public void export(PdfExportRequest request) {
        log.info("PDF出力を開始します: imageCount={}, outputPath={}",
                request.getImageIds().size(), request.getOutputPath());

        // 画像エンティティを取得
        List<Image> images = fetchImages(request.getImageIds());
        if (images.isEmpty()) {
            throw new PdfExportException("出力対象の画像がありません");
        }

        PDRectangle pageSize = resolvePaperSize(request.getPaperSize());
        int imagesPerPage = Math.max(1, request.getImagesPerPage());

        try (PDDocument document = new PDDocument()) {
            // 画像をページに配置
            for (int i = 0; i < images.size(); i += imagesPerPage) {
                PDPage page = new PDPage(pageSize);
                document.addPage(page);
                List<Image> pageImages = images.subList(i,
                        Math.min(i + imagesPerPage, images.size()));
                addImagesToPage(document, page, pageImages, imagesPerPage, pageSize);
            }

            document.save(new File(request.getOutputPath()));
            log.info("PDF出力が完了しました: outputPath={}, pages={}",
                    request.getOutputPath(), document.getNumberOfPages());
        } catch (IOException e) {
            log.error("PDF出力に失敗しました: outputPath={}", request.getOutputPath(), e);
            throw new PdfExportException("PDF出力に失敗しました: " + e.getMessage(), e);
        }
    }

    /**
     * 画像IDリストから画像エンティティのリストを取得する。
     *
     * @param imageIds 画像IDリスト
     * @return 画像エンティティのリスト
     * @throws ImageNotFoundException 指定されたIDの画像が存在しない場合
     */
    private List<Image> fetchImages(List<Long> imageIds) {
        List<Image> images = new ArrayList<>();
        for (Long id : imageIds) {
            Image image = imageRepository.findById(id)
                    .orElseThrow(() -> ImageNotFoundException.ofId(id));
            // 動画ファイルはPDF出力対象外
            if ("image".equals(image.getMediaType())) {
                images.add(image);
            } else {
                log.debug("動画ファイルはPDF出力対象外のためスキップします: id={}", id);
            }
        }
        return images;
    }

    /**
     * ページに画像を配置する。
     *
     * @param document      PDFドキュメント
     * @param page          配置先ページ
     * @param pageImages    配置する画像リスト
     * @param imagesPerPage 1ページあたりの最大枚数
     * @param pageSize      ページサイズ
     * @throws IOException 画像の読み込みまたは配置に失敗した場合
     */
    private void addImagesToPage(PDDocument document, PDPage page, List<Image> pageImages,
                                  int imagesPerPage, PDRectangle pageSize) throws IOException {
        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            float pageWidth = pageSize.getWidth();
            float pageHeight = pageSize.getHeight();
            float margin = 20f;

            // グリッドレイアウトを計算
            int cols = (int) Math.ceil(Math.sqrt(imagesPerPage));
            int rows = (int) Math.ceil((double) imagesPerPage / cols);
            float cellWidth = (pageWidth - margin * (cols + 1)) / cols;
            float cellHeight = (pageHeight - margin * (rows + 1)) / rows;

            for (int i = 0; i < pageImages.size(); i++) {
                Image image = pageImages.get(i);
                File imageFile = new File(image.getFilePath());
                if (!imageFile.exists()) {
                    log.warn("画像ファイルが見つかりません: {}", image.getFilePath());
                    continue;
                }

                try {
                    PDImageXObject pdImage = PDImageXObject.createFromFile(
                            image.getFilePath(), document);

                    // セルの位置を計算
                    int col = i % cols;
                    int row = i / cols;
                    float x = margin + col * (cellWidth + margin);
                    float y = pageHeight - margin - (row + 1) * (cellHeight + margin) + margin;

                    // アスペクト比を維持してフィット
                    float[] fitSize = fitImageInCell(
                            pdImage.getWidth(), pdImage.getHeight(), cellWidth, cellHeight);
                    float imgX = x + (cellWidth - fitSize[0]) / 2;
                    float imgY = y + (cellHeight - fitSize[1]) / 2;

                    contentStream.drawImage(pdImage, imgX, imgY, fitSize[0], fitSize[1]);
                } catch (IOException e) {
                    log.warn("画像の配置に失敗しました: {}", image.getFilePath(), e);
                }
            }
        }
    }

    /**
     * 画像をセルにフィットさせるサイズを計算する。
     *
     * @param imgWidth   元画像の幅
     * @param imgHeight  元画像の高さ
     * @param cellWidth  セルの幅
     * @param cellHeight セルの高さ
     * @return フィットさせた後の [幅, 高さ]
     */
    private float[] fitImageInCell(float imgWidth, float imgHeight,
                                    float cellWidth, float cellHeight) {
        float scaleX = cellWidth / imgWidth;
        float scaleY = cellHeight / imgHeight;
        float scale = Math.min(scaleX, scaleY);
        return new float[]{imgWidth * scale, imgHeight * scale};
    }

    /**
     * 用紙サイズ名から {@link PDRectangle} を返す。
     *
     * @param paperSize 用紙サイズ名（例: {@code "A4"}, {@code "A3"}, {@code "Letter"}）
     * @return PDRectangle
     */
    private PDRectangle resolvePaperSize(String paperSize) {
        if (paperSize == null) return PDRectangle.A4;
        return switch (paperSize.toUpperCase()) {
            case "A3" -> PDRectangle.A3;
            case "B4" -> new PDRectangle(728.5f, 1031.8f);
            case "B5" -> new PDRectangle(515.9f, 728.5f);
            case "LETTER" -> PDRectangle.LETTER;
            default -> PDRectangle.A4;
        };
    }
}
