package com.citygo;
import com.citygo.file.storage.*;
import com.citygo.file.service.*;
import com.citygo.auth.security.LoginUser;
import com.citygo.common.exception.BizException;
import io.minio.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.*;
import org.springframework.mock.web.MockMultipartFile;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
/** Explicit opt-in: mvnw -Dtest=S3FileUploadAcceptanceIT test against real local MinIO, never silently skipped. */
@TestPropertySource(properties="citygo.storage.type=s3")
class S3FileUploadAcceptanceIT extends FileUploadIntegrationTest {
 static final String bucket="citygo-acceptance-"+UUID.randomUUID().toString().replace("-","");
 static String required(String name){String value=System.getenv(name);if(value==null||value.isBlank())throw new IllegalStateException("MinIO acceptance requires "+name);return value;}
 static MinioClient client(){return MinioClient.builder().endpoint("http://127.0.0.1:9000").credentials(required("CITYGO_STORAGE_S3_ACCESS_KEY"),required("CITYGO_STORAGE_S3_SECRET_KEY")).build();}
 @DynamicPropertySource static void storage(DynamicPropertyRegistry r){r.add("citygo.storage.type",()->"s3");r.add("citygo.storage.s3.endpoint",()->"http://127.0.0.1:9000");r.add("citygo.storage.s3.access-key",()->required("CITYGO_STORAGE_S3_ACCESS_KEY"));r.add("citygo.storage.s3.secret-key",()->required("CITYGO_STORAGE_S3_SECRET_KEY"));r.add("citygo.storage.s3.bucket",()->bucket);}
 @Autowired StorageService storage;@Autowired FileService files;@Autowired FileCleanupJob cleanup;
 static byte[] png()throws Exception{var out=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(3,3,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",out);return out.toByteArray();}
 @Test void actualS3BytesMetadataAndDeletionRoundTrip()throws Exception{
 assertThat(storage).isInstanceOf(S3StorageService.class);byte[] bytes=png();Long uid=insertUser("s3-roundtrip","secret123","ADMIN",true);var user=new LoginUser(uid,"s3-roundtrip","ADMIN","s3");
 var uploaded=files.upload(user,new MockMultipartFile("file","actual.png","text/html",bytes));String key=uploaded.url().substring("/api/files/".length());
 try(var body=client().getObject(GetObjectArgs.builder().bucket(bucket).object(key).build())){assertThat(body.readAllBytes()).isEqualTo(bytes);}
 assertThat(client().statObject(StatObjectArgs.builder().bucket(bucket).object(key).build()).contentType()).isEqualTo("image/png");
 files.delete(user,uploaded.id());cleanup.cleanup();assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM file_objects WHERE id=?",Integer.class,uploaded.id())).isZero();assertThatThrownBy(()->client().statObject(StatObjectArgs.builder().bucket(bucket).object(key).build())).isInstanceOf(io.minio.errors.ErrorResponseException.class);
 }
 @Test void s3RejectsUnsafeKeysAndMissingObjectIsNotFound(){assertThatThrownBy(()->storage.load("../private.txt")).isInstanceOf(BizException.class).extracting(e->((BizException)e).getErrorCode()).isEqualTo(com.citygo.common.api.ErrorCode.BAD_REQUEST);assertThatThrownBy(()->storage.load("2026/10/missing.png")).isInstanceOf(BizException.class).extracting(e->((BizException)e).getErrorCode()).isEqualTo(com.citygo.common.api.ErrorCode.NOT_FOUND);}
 @Test void unavailableS3IsRetryableNotFalseNotFound(){var offline=new S3StorageService("http://127.0.0.1:1","acceptance","acceptance-secret","unavailable");assertThatThrownBy(()->offline.load("2026/10/a.png")).isInstanceOf(BizException.class).extracting(e->((BizException)e).getErrorCode()).isEqualTo(com.citygo.common.api.ErrorCode.SERVICE_UNAVAILABLE);}
 @Test void acceptedObjectSurvivesLocalContainerRestart()throws Exception{
 String key="2026/10/restart-"+UUID.randomUUID()+".png";byte[] bytes=png();storage.store(new MockMultipartFile("file","restart.png","image/png",bytes),key);
 var process=new ProcessBuilder("docker","restart","citygo-minio").redirectErrorStream(true).start();boolean finished=process.waitFor(30,java.util.concurrent.TimeUnit.SECONDS);assertThat(finished).isTrue();assertThat(process.exitValue()).isZero();
 boolean restored=false;for(int attempt=0;attempt<30;attempt++){try{assertThat(storage.load(key).getContentAsByteArray()).isEqualTo(bytes);restored=true;break;}catch(BizException transientFailure){Thread.sleep(300);}}
 assertThat(restored).isTrue();
 }
 @AfterAll static void removeOnlyOwnAcceptanceBucket()throws Exception{var c=client();if(!c.bucketExists(BucketExistsArgs.builder().bucket(bucket).build()))return;for(var result:c.listObjects(ListObjectsArgs.builder().bucket(bucket).recursive(true).build()))c.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(result.get().objectName()).build());c.removeBucket(RemoveBucketArgs.builder().bucket(bucket).build());}
}
