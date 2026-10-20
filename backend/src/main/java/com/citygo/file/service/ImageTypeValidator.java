package com.citygo.file.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;

/**
 * 图片真实类型校验：白名单 jpg/jpeg/png/webp，
 * 按文件头魔数核对，拒绝伪造扩展名（实施书 §12）。
 */
@Slf4j
public final class ImageTypeValidator {

    public static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;
    private static final java.util.Set<String> ALLOWED_EXT =
            java.util.Set.of("jpg", "jpeg", "png", "webp");

    private ImageTypeValidator() {
    }

    public static String requireAllowedExt(String originalName) {
        if (originalName == null || !originalName.contains(".")) {
            throw new IllegalArgumentException("仅支持 jpg/jpeg/png/webp 图片");
        }
        String ext = originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase();
        if (!ALLOWED_EXT.contains(ext)) {
            throw new IllegalArgumentException("仅支持 jpg/jpeg/png/webp 图片");
        }
        return ext;
    }

    /** 读前 12 字节核对魔数；ext 必须与真实类型一致。 */
    public static void requireMagicMatches(String ext, MultipartFile file) {
        byte[] head = new byte[12];
        try (var in = file.getInputStream()) {
            int read = in.read(head);
            if (read < 12) {
                throw new IllegalArgumentException("文件内容与扩展名不符");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("文件读取失败");
        }
        boolean ok = switch (ext) {
            case "jpg", "jpeg" -> isJpeg(head);
            case "png" -> isPng(head);
            case "webp" -> isWebp(head);
            default -> false;
        };
        if(ok && ext.equals("webp")){try{byte[] b=file.getBytes();long size=Integer.toUnsignedLong(java.nio.ByteBuffer.wrap(b,4,4).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt());if(b.length<30||size+8!=b.length||b[12]!='V'||b[13]!='P'||b[14]!='8'||(b[15]!=' '&&b[15]!='L'&&b[15]!='X'))throw new IllegalArgumentException("WebP 图片内容无效");}catch(IOException e){throw new IllegalArgumentException("图片读取失败");}}
        if (ok && !ext.equals("webp")) {
            try(var input=javax.imageio.ImageIO.createImageInputStream(file.getInputStream())){
                var readers=javax.imageio.ImageIO.getImageReaders(input);if(!readers.hasNext())throw new IllegalArgumentException("图片内容无效");
                var reader=readers.next();try{reader.setInput(input);long pixels=(long)reader.getWidth(0)*reader.getHeight(0);if(pixels<=0||pixels>25000000)throw new IllegalArgumentException("图片尺寸过大或无效");if(reader.read(0)==null)throw new IllegalArgumentException("图片内容无效");}finally{reader.dispose();}
            }catch(IOException e){throw new IllegalArgumentException("图片内容无效");}
        }
        if (!ok) {
            throw new IllegalArgumentException("文件内容与扩展名不符");
        }
    }

    private static boolean isJpeg(byte[] h) {
        return (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF;
    }

    private static boolean isPng(byte[] h) {
        byte[] sig = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        return Arrays.equals(Arrays.copyOf(h, 8), sig);
    }

    private static boolean isWebp(byte[] h) {
        return h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P';
    }
}
