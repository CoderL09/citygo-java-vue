package com.citygo;
import com.citygo.file.service.*;
import com.citygo.file.storage.StorageService;
import com.citygo.file.mapper.FileObjectMapper;
import com.citygo.auth.security.LoginUser;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.mock.web.MockMultipartFile;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class FileCleanupIntegrationTest extends P2Fixtures {
 @Autowired FileService files;@Autowired FileCleanupJob job;@Autowired FileObjectMapper mapper;@MockitoSpyBean StorageService storage;long uid,id;String key;
 @BeforeEach void fixture()throws Exception{uid=insertUser("cleaner","secret123","ADMIN",true);var out=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",out);id=files.upload(new LoginUser(uid,"cleaner","ADMIN","cleaner"),new MockMultipartFile("file","cover.png","image/png",out.toByteArray())).id();key=mapper.findById(id).getStorageKey();}
 @Test void gracePeriodProtectsNewFilesAndReferencedFiles(){job.cleanup();assertThat(mapper.findById(id)).isNotNull();long a=activity();jdbcTemplate.update("INSERT INTO activity_images(activity_id,file_object_id,sort_order,created_at) VALUES(?,?,0,UTC_TIMESTAMP(3))",a,id);jdbcTemplate.update("UPDATE file_objects SET unreferenced_at=UTC_TIMESTAMP(3)-INTERVAL 2 DAY WHERE id=?",id);job.cleanup();assertThat(mapper.findById(id)).isNotNull();assertThat(count("activity_images")).isEqualTo(1);}
 @Test void cleanupRetriesStorageFailureAndAuditsOnlySuccessfulDeletion(){jdbcTemplate.update("UPDATE file_objects SET unreferenced_at=UTC_TIMESTAMP(3)-INTERVAL 2 DAY WHERE id=?",id);doThrow(new IllegalStateException("simulated storage unavailable")).when(storage).delete(key);job.cleanup();assertThat(mapper.findById(id).getStatus()).isEqualTo("DELETING");assertThat(count("job_failures")).isEqualTo(1);reset(storage);job.cleanup();job.cleanup();assertThat(mapper.findById(id)).isNull();assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM audit_logs WHERE action='cleanup'",Integer.class)).isEqualTo(1);}
 @Test void forgedMagicWithoutDecodableImageIsRejected(){byte[] b={(byte)137,80,78,71,13,10,26,10,0,0,0,0,0,0,0,0};assertThatThrownBy(()->files.upload(new LoginUser(uid,"cleaner","ADMIN","cleaner"),new MockMultipartFile("file","fake.png","image/png",b))).isInstanceOf(com.citygo.common.exception.BizException.class);}
}
