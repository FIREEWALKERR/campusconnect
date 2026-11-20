package edu.campusconnect.service;

import edu.campusconnect.entity.*;
import edu.campusconnect.enums.*;
import edu.campusconnect.exception.BusinessException;
import edu.campusconnect.repository.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipInputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ResumeService {
  private final StudentProfileRepository students;
  private final ApplicationRepository applications;
  private final AccessService access;

  @Value("${app.resume-directory}")
  private String directory;

  @Value("${spring.servlet.multipart.max-file-size}")
  private org.springframework.util.unit.DataSize maximumSize;

  @Transactional
  public void upload(MultipartFile file) throws IOException {
    access.roles(Role.STUDENT);
    var s = students.findByUserId(access.current().getId()).orElseThrow();
    if (file.isEmpty() || file.getSize() > maximumSize.toBytes())
      throw new BusinessException(
          "Resume must be nonempty and no larger than " + maximumSize.toBytes() + " bytes");
    String name =
        Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT);
    String type = Optional.ofNullable(file.getContentType()).orElse("");
    byte[] bytes = file.getBytes();
    String extension;
    if (name.endsWith(".pdf")
        && type.equals("application/pdf")
        && bytes.length > 5
        && new String(bytes, 0, 5, java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-"))
      extension = ".pdf";
    else if (name.endsWith(".docx")
        && type.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
        && docx(bytes)) extension = ".docx";
    else throw new BusinessException("Upload a valid PDF or DOCX with its correct content type");
    Path root = Path.of(directory).toAbsolutePath().normalize();
    Files.createDirectories(root);
    String key = UUID.randomUUID() + extension;
    Path dest = root.resolve(key);
    Files.write(dest, bytes, StandardOpenOption.CREATE_NEW);
    String old = s.getResumeKey();
    s.setResumeKey(key);
    students.saveAndFlush(s);
    // Keep the old file until database commit; rollback removes the new file.
    org.springframework.transaction.support.TransactionSynchronizationManager
        .registerSynchronization(
            new org.springframework.transaction.support.TransactionSynchronization() {
              @Override
              public void afterCompletion(int status) {
                try {
                  if (status == STATUS_COMMITTED) {
                    if (old != null) Files.deleteIfExists(safe(root, old));
                  } else Files.deleteIfExists(dest);
                } catch (IOException ignored) {
                  /* orphan cleanup may be retried by an operator */
                }
              }
            });
  }

  private boolean docx(byte[] bytes) {
    boolean content = false, document = false;
    long total = 0;
    int count = 0;
    try (var zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
      java.util.zip.ZipEntry e;
      byte[] buf = new byte[8192];
      while ((e = zip.getNextEntry()) != null) {
        if (++count > 500) return false;
        if (e.getName().equals("[Content_Types].xml")) content = true;
        if (e.getName().equals("word/document.xml")) document = true;
        if (e.getName().toLowerCase(Locale.ROOT).contains("vbaproject")) return false;
        int n;
        while ((n = zip.read(buf)) != -1) {
          total += n;
          if (total > 25 * 1024 * 1024) return false;
        }
      }
    } catch (IOException e) {
      return false;
    }
    return content && document;
  }

  private static Path safe(Path root, String key) {
    if (!key.matches("[a-f0-9-]{36}\\.(pdf|docx)"))
      throw new BusinessException("Invalid resume reference");
    Path p = root.resolve(key).normalize();
    if (!p.startsWith(root)) throw new BusinessException("Invalid resume reference");
    return p;
  }

  @Transactional(readOnly = true)
  public Path download(Long id) {
    var s = students.findById(id).orElseThrow();
    var u = access.current();
    boolean allowed =
        access.staff(u)
            || s.getUser().getId().equals(u.getId())
            || applications.findByStudentId(id).stream()
                .anyMatch(a -> access.manages(u, a.getJob()));
    if (!allowed)
      throw new org.springframework.security.access.AccessDeniedException("Resume is private");
    if (s.getResumeKey() == null) throw new BusinessException("No resume has been uploaded");
    Path p = safe(Path.of(directory).toAbsolutePath().normalize(), s.getResumeKey());
    if (!Files.isRegularFile(p))
      throw new BusinessException("Resume file is unavailable; please upload it again");
    return p;
  }
}
