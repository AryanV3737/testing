package com.minfy.s3file;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.UUID;
import java.util.Base64;

public class UploadToS3Bucket implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final String BUCKET_NAME = "mys3testbuck1"; // write the bucket name here
    private static final String REGION = "ap-south-1"; // write the region name here

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        try {
            // Initialize S3 client
            AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                    .withRegion(REGION)
                    .build();

            // Generate a unique object key (e.g., using UUID)
            String objectKey = "uploads/" + UUID.randomUUID().toString() + ".pdf";

            // Extract the base64-encoded file content from the HTTP request body
            String base64FileContent = request.getBody();

            // Decode the base64 content into a byte array
            byte[] fileData = Base64.getDecoder().decode(base64FileContent);

            // Create an input stream from the byte array
            InputStream inputStream = new ByteArrayInputStream(fileData);

            // Set the content type
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType("application/pdf");

            // Set the content length
            metadata.setContentLength(fileData.length);

            // Upload the file to the S3 bucket
            s3Client.putObject(new PutObjectRequest(BUCKET_NAME, objectKey, inputStream, metadata));

            // Generate a presigned URL for the uploaded object
            Date expiration = new Date(System.currentTimeMillis() + 3600000); // URL expiration time (1 hour)
            GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(BUCKET_NAME, objectKey)
                    .withMethod(HttpMethod.GET)
                    .withExpiration(expiration);

            URL presignedUrl = s3Client.generatePresignedUrl(generatePresignedUrlRequest);

            // Build the response
            APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
            response.setStatusCode(200);
            response.setBody("File uploaded successfully to S3: " + objectKey);

            // Include the presigned URL in the response
            response.setHeaders(java.util.Collections.singletonMap("Presigned-URL", presignedUrl.toString()));

            return response;
        } catch (Exception e) {
            e.printStackTrace();
            // Return an error response
            APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
            response.setStatusCode(500);
            response.setBody("Error: " + e.getMessage());

            return response;
        }
    }
}
