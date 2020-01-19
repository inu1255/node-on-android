package cn.inu1255.soulsign.proxy.tunnel;

import java.nio.ByteBuffer;

public interface IEncryptor {

    void encrypt(ByteBuffer buffer);

    void decrypt(ByteBuffer buffer);

}
