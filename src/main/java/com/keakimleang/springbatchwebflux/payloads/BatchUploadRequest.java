package com.keakimleang.springbatchwebflux.payloads;

import org.springframework.http.codec.multipart.*;

public record BatchUploadRequest(FilePart file,
                                 String batchOwnerName,
                                 boolean runJobAsync) {
}
