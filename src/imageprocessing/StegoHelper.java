package imageprocessing; /**
 * This is a Helper class for Image hiding methods
 * This helps to hide a message image in a carrier image
 *
 * DATE:        October 12, 2016
 *
 * REVISIONS:
 *
 * DESIGNER:    Vishav Singh <vishav27@gmail.com>
 *
 * PROGRAMMER:  Vishav Singh <vishav27@gmail.com>
 *
 */


import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

/**
 * Created by vishavpreetsingh on 2016-10-10.
 */
public class StegoHelper
{
    /**
     * Default Constructor
     */
    ImageProcessor imageProcessor = new ImageProcessor();
    public StegoHelper()
    {

    }

    /**
     * Return proper byte format of an integer
     * @param i Integer to convert
     * @return  Return a byte[4] array containing the ocnverted integer
     */
    private byte[] bit_conversion(int i)
    {
        byte byte3 = (byte)((i & 0xFF000000) >>> 24);
        byte byte2 = (byte)((i & 0x00FF0000) >>> 16);
        byte byte1 = (byte)((i & 0x0000FF00) >>> 8 );
        byte byte0 = (byte)((i & 0x000000FF)       );

        return(new byte[]{byte3,byte2,byte1,byte0});
    }

    /**
     *To encode the stego image in a carrier image
     * @param pathCarrier   Path of the Image to hide the message Image
     * @param nameCarrier   Name of the Image to hide the message Image
     * @param extCarrier    Extension of the Carrier Image
     * @param pathMessage   Path of the Image to hide
     * @param nameMessage   Name of the Image to hide
     * @param extMessage    Extension of the image to hide
     * @param stegoImage    Name of the new Image containg the carrier and stego image
     * @return              True on success
     */
    public boolean encode(String pathCarrier, String nameCarrier, String extCarrier,
                          String pathMessage, String nameMessage, String extMessage, String stegoImage, byte[] password)
    {

        String  carrier_file_name   =   getPath(pathCarrier, nameCarrier, extCarrier);// The carier image
        String  message_file_name   =   getPath(pathMessage, nameMessage, extMessage);// The image to hide

        BufferedImage carrier_image_original    =   imageProcessor.getImage(carrier_file_name);
        BufferedImage message_image_original    =   imageProcessor.getImage(message_file_name);
        BufferedImage carrier_image_memory      =   imageProcessor.copy_image(carrier_image_original);
        BufferedImage message_image_memory      =   imageProcessor.copy_image(message_image_original);
        byte[] payloadBytes = get_message_bytes(pathMessage,nameMessage,extMessage);



        byte[] encryptedMessage = null;
        try

        {
            encryptedMessage = encryptPayload(payloadBytes, password);
        }catch(Exception ex)
        {
            System.out.println("Error while encrypting");
        }
        //carrier_image_memory = add_image(carrier_image_original, message_image_original);
        System.out.println("the length of the encrypted message " + encryptedMessage.length);
        carrier_image_memory = add_image(carrier_image_memory, encryptedMessage);
        return imageProcessor.setImage(carrier_image_memory, new File(getPath(pathCarrier+"/stego",stegoImage,extMessage)), extMessage);
    }

    /**
     * To decode the Hidden Image
     * @param path          Path of the image to decode
     * @param name          Name of the image to decode
     * @param extension     Extension of the image to decode
     * @param new_path      Path of the new image file to be created
     * @param new_image     Name of the new image file
     * @param password      Passowrd to decrypt
     */
    public void decode(String path, String name, String extension,String new_path ,String new_image, byte[] password)
    {
        byte[] decode;
        byte[] decryptedMessage;

        String stego_file = getPath(path,name,extension);
        try
        {
            BufferedImage image = imageProcessor.getImage(stego_file);
            BufferedImage workableImage = imageProcessor.copy_image(image);

            decode = decode_image(imageProcessor.get_bytes(image));
            decryptedMessage = decryptPayload(decode,password);

            System.out.println("Legth of decoded message: " + decryptedMessage.length);

            String hi_path = getPath(new_path,new_image,extension);

            FileOutputStream fos = new FileOutputStream(hi_path);
            fos.write(decryptedMessage);
            fos.close();

        }catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }


    /**
     *To Decode the Image from a carrier image
     * @param image The Image that contains a stego image
     * @return      The byte array of the stego image
     */
    public byte[] decode_image(byte[] image)
    {
        int length = 0;
        int offset = 32;

        for (int i=0; i<32; ++i)
        {
            length = (length << 1) | (image[i] & 1);
        }

        System.out.println("The Size of Message Image: " + length);
        System.out.println("Lenght of actual image is : " + image.length);
        byte[] result = new byte[length];

        for(int b=0; b<result.length; ++b )
        {
            for(int i=0; i<8; ++i, ++offset)
                result[b] = (byte) ((result[b] << 1) | (image[offset] & 1));
        }
        return result;

    }

    /**
     * Builds the File Name
     * @param path      The path containing the image
     * @param title     The title of the image
     * @param extension The extension of the image
     * @return          The complete path of the image
     */
    public String getPath(String path, String title, String extension)
    {
        return path + "/" +title + "."+extension;
    }


    /**
     * To add the message Image into the Carrier Image
     * @param carrierImage  The image to hide the message image
     * @param messageImage  The image to hide
     * @return              The image with the message image embedded in it
     */
    public BufferedImage add_image(BufferedImage carrierImage, BufferedImage messageImage)
    {
        byte img_carrier[]  =   imageProcessor.get_bytes(carrierImage);
        byte img_message[]  =   imageProcessor.get_bytes(messageImage);
        byte len[]          =   bit_conversion(img_message.length);
        System.out.println("The length to encode is: " + img_message.length);
        try
        {
            encode_image(img_carrier, len,0);
            encode_image(img_carrier, img_message, 32);
        }catch (Exception ex)
        {
            ex.printStackTrace();
            System.out.println("Error While Adding Image!!!");
        }

        return carrierImage;
    }


    /**
     * To add the message   Image into the Carrier Image
     * @param carrierImage  The image to hide the message image
     * @param message       The byte array to hide
     * @return              The image with the message image embedded in it
     */
    public BufferedImage add_image(BufferedImage carrierImage, byte[] message)
    {

        byte img_carrier[]  =   imageProcessor.get_bytes(carrierImage);
        byte len[]          =   bit_conversion(message.length);
        System.out.println("The length to encode is: " + message.length);

        try
        {
            encode_image(img_carrier, len,0);
            encode_image(img_carrier, message, 32);
        }catch (Exception ex)
        {
            ex.printStackTrace();
            System.out.println("Error While Adding Image!!!");
        }

        return carrierImage;
    }

    /**
     * To embedd the Message in the Carrier Image in the LSB
     * @param carrierImage  Array of bytes for Carrier Image
     * @param data          Array of bytes for Message Image
     * @param offset        Offset from where to start the embedding in the carrier image
     * @return              The embedded byte array containing the Carrier and the Data
     */
    public byte[] encode_image(byte[] carrierImage, byte[] data, int offset)
    {
        if(data.length + offset >= carrierImage.length)
        {
            throw  new IllegalArgumentException("Carrier Image not long enough!!!!");
        }

        for (int i=0; i < data.length; ++i)
        {
            int add = data[i];
            for (int bit=7; bit>=0; --bit, ++offset)
            {
                int b = (add >>> bit) & 1;
                carrierImage[offset] = (byte)((carrierImage[offset] & 0xFE) | b);
            }
        }
        return carrierImage;
    }


    /**
     * To get the byte array of a file
     * @param filePath  The path of the file
     * @param fileName  The name of the file
     * @param ext       The Extension of the file
     * @return          The byte  array of the file
     */
    public byte[] get_message_bytes(String filePath, String fileName, String ext)
    {
        byte[] payloadBytes = null;
        try {
            payloadBytes = Files.readAllBytes(new File(getPath(filePath,fileName,ext)).toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return  payloadBytes;
    }

    /**
     * To encrypt the byte array Payload. Uses AES cipher
     * @param payload   The byte array to encrypt
     * @param password  The passowrd for encryption
     * @return          The encrypted byte array
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws IllegalStateException
     * @throws ShortBufferException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    public byte[] encryptPayload(byte[] payload, byte[] password)throws
        NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException,
        InvalidKeyException, IllegalStateException, ShortBufferException,
        IllegalBlockSizeException, BadPaddingException
    {
        SecretKeySpec key   =   new SecretKeySpec(password,"AES");
        Cipher cipher       =   Cipher.getInstance("AES");

        cipher.init(Cipher.ENCRYPT_MODE,key);

        byte[]  cipherText      =   new byte[cipher.getOutputSize(payload.length)];
        int     ctLength        =   cipher.update(payload, 0, payload.length, cipherText,0);
        System.out.println("Length of cipher text " + cipherText.length);

        ctLength += cipher.doFinal(cipherText, ctLength);
        return cipherText;
    }

    /**
     *To decrypt the encrypted byte array
     * @param payload   The encrypted byte array
     * @param password  The password for decryption
     * @return          The decrypted byte array
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws IllegalStateException
     * @throws ShortBufferException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    public byte[] decryptPayload(byte[] payload, byte[] password) throws NoSuchAlgorithmException,
        NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, IllegalStateException, ShortBufferException,
        IllegalBlockSizeException, BadPaddingException
    {
        SecretKeySpec key   = new SecretKeySpec(password, "AES");
        Cipher cipher       = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] image        = new byte[cipher.getOutputSize(payload.length)];
        int ptLength        = cipher.update(payload, 0, payload.length, image, 0);
        ptLength            += cipher.doFinal(image, ptLength);
        return image;
    }

}
