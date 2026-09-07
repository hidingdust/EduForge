package com.woniuxy.utils;


import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.woniuxy.utils.enums.TokenEnum;

import java.util.Date;

public class JWTUtils {
    public static final String SECRET_KEY = "myh123456"; //秘钥
    public static final long TOKEN_EXPIRE_TIME = 7 * 24 * 60 * 60 * 1000; //token过期时间 7天
    private static final String ISSUER = "woniuxy"; //签发人

    /**
     * 生成签名
     */
    public static String generateToken(String uid) {
        Date now = new Date();        //创建签名算法对象
        Algorithm algorithm = Algorithm.HMAC256(SECRET_KEY); //算法
        String token = JWT.create()
                .withIssuer(ISSUER) //签发人
                .withIssuedAt(now)  //签发时间
                .withExpiresAt(new Date(now.getTime() + TOKEN_EXPIRE_TIME)) //过期时间，可由redis来控制
                .withClaim("uid", uid) //保存身份标识
                .sign(algorithm);
        return token;
    }

    /**
     * 验证token
     */
    public static TokenEnum verify(String token) {
        try {
            //签名算法
            Algorithm algorithm = Algorithm.HMAC256(SECRET_KEY); //算法
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer(ISSUER)
                    .build();
            verifier.verify(token);
            return TokenEnum.TOKEN_SUCCESS;
        } catch (TokenExpiredException ex) {
            return TokenEnum.TOKEN_EXPIRE;
            //ex.printStackTrace();
        } catch (Exception e) {
            return TokenEnum.TOKEN_BAD;
        }
    }

    /**
     * 从token获取uid
     */
    public static String getUid(String token) {
        try {
            return JWT.decode(token).getClaim("uid").asString();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "";
    }

    // 解析token获取里面存放的phone手机号
    public static String getPhone(String token) {
        try {
            return JWT.decode(token).getClaim("phone").asString();
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
    /**
     * 从Authorization请求头直接提取token内存储的手机号（自动剥离Bearer）
     */
    public static String getPhoneFromAuthHeader(String rawAuthHeader) {
        if (rawAuthHeader == null || rawAuthHeader.isBlank()) {
            throw new RuntimeException("未携带登录凭证");
        }
        String token = rawAuthHeader;
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        TokenEnum tokenEnum = verify(token);
        if (!TokenEnum.TOKEN_SUCCESS.equals(tokenEnum)) {
            throw new RuntimeException("登录凭证失效，请重新登录");
        }
        return getUid(token);
    }
}