package service;

import exception.McpErrorCode;
import exception.McpException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class PaperService {

    /**
     * 读取 PDF 文件内容
     *
     * @param filePath 文件路径
     * @return String 文件内容
     */
    public String readPdfContent(String filePath) {

        // 参数判空
        if (filePath == null || filePath.isBlank()) {
            throw new McpException(McpErrorCode.INVALID_PARAMS, "File path cannot be empty");
        }

        Path path = Paths.get(filePath);
        File file = path.toFile();

        // 文件是否存在
        if (!Files.exists(path)) {
            throw new McpException(McpErrorCode.FILE_NOT_FOUND, "PDF file not found: " + filePath);
        }

        // 是否为文件（防止传入文件夹路径）
        if (!Files.isRegularFile(path)) {
            throw new McpException(McpErrorCode.INVALID_PARAMS, "Path is not a file: " + filePath);
        }

        // PDFBox 读取
        try (PDDocument document = Loader.loadPDF(file)) {
            // 检查是否加密（有些论文会有权限设置）
            if (document.isEncrypted()) {
                // 如果需要密码才能读，抛出权限错误
                throw new McpException(McpErrorCode.PERMISSION_DENIED,
                        "The PDF is encrypted and cannot be read without a password.");
            }

            PDFTextStripper stripper = new PDFTextStripper();

            // 可选优化：设置排序，防止双栏排版读乱序
            stripper.setSortByPosition(true);

            String text = stripper.getText(document);

            // 内容是否为空（可能是全图片扫描件）
            if (text == null || text.isBlank()) {
                throw new McpException(McpErrorCode.INTERNAL_ERROR,
                        "PDF parsed successfully but contains no text (It might be a scanned image).");
            }

            return text;

        } catch (InvalidPasswordException e) {
            // 捕获密码错误
            throw new McpException(McpErrorCode.PERMISSION_DENIED, "PDF is encrypted: " + e.getMessage());

        } catch (IOException e) {
            // 捕获其他 IO 错误（如文件损坏、被占用）
            throw new McpException(McpErrorCode.INTERNAL_ERROR, "Failed to read PDF file: " + e.getMessage());
        }
    }


}