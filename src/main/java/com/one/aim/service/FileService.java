package com.one.aim.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.one.aim.bo.AttachmentBO;
import com.one.aim.bo.FileBO;
import com.one.aim.rq.AttachmentRq;
import com.one.vm.core.BaseRs;

public interface FileService {

    BaseRs uploadFile(MultipartFile file) throws Exception;

    FileBO downloadFile(String id) throws Exception;

    BaseRs deleteFileById(String fileId) throws Exception;

    byte[] getContentFromGridFS(String fileId) throws Exception;

    List<AttachmentBO> prepareAttBOs(List<AttachmentRq> vms, String userName);

    FileBO uploadAndReturnFile(MultipartFile file) throws Exception;

    FileBO uploadBytes(byte[] data, String filename) throws IOException;

    FileBO getFile(String id) throws Exception;

    void deleteFile(Long fileId) throws Exception;

    String getPublicFileUrl(Long fileId);


    String getPublicViewUrl(Long fileId);


    FileBO uploadFile(InputStream inputStream, String fileName, String contentType) throws Exception;



}
