package com.wbtracker.app.di

import com.wbtracker.app.data.remote.WbApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.ConnectionPool
import okhttp3.Dns
import okhttp3.OkHttpClient
import java.net.Inet4Address
import java.net.InetAddress
import java.net.UnknownHostException
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    class MincifraTrustManager : X509TrustManager {
        private val systemTrustManager: X509TrustManager

        init {
            val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            factory.init(null as KeyStore?)
            systemTrustManager = factory.trustManagers.first { it is X509TrustManager } as X509TrustManager
        }

        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            systemTrustManager.checkClientTrusted(chain, authType)
        }

        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            try {
                systemTrustManager.checkServerTrusted(chain, authType)
            } catch (e: CertificateException) {
                val cert = chain?.get(0)
                val issuer = cert?.issuerDN?.name ?: ""
                if (issuer.contains("Russian Trusted", ignoreCase = true) ||
                    issuer.contains("Mincifra", ignoreCase = true) ||
                    issuer.contains("MinSvyaz", ignoreCase = true)
                ) {
                    return
                }
                throw e
            }
        }

        override fun getAcceptedIssuers(): Array<X509Certificate> = systemTrustManager.acceptedIssuers
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val trustManager = MincifraTrustManager()

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(trustManager), SecureRandom())
        val sslSocketFactory = sslContext.socketFactory

        val userAgents = listOf(
            "Mozilla/5.0 (Linux; Android 13; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Mobile Safari/537.36",
            "Mozilla/5.0 (Linux; Android 12; Pixel 6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Mobile Safari/537.36",
            "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        )

        return OkHttpClient.Builder()
            .sslSocketFactory(sslSocketFactory, trustManager)
            .hostnameVerifier { hostname, session ->
                val isWbDomain = hostname == "wb.ru" || hostname.endsWith(".wb.ru") ||
                        hostname == "wbbasket.ru" || hostname.endsWith(".wbbasket.ru") ||
                        hostname == "wildberries.ru" || hostname.endsWith(".wildberries.ru")
                if (isWbDomain) {
                    true
                } else {
                    javax.net.ssl.HttpsURLConnection.getDefaultHostnameVerifier().verify(hostname, session)
                }
            }
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .writeTimeout(8, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
            .dns(object : Dns {
                override fun lookup(hostname: String): List<InetAddress> {
                    return try {
                        val addresses = InetAddress.getAllByName(hostname).toList()
                        addresses.sortedBy { if (it is Inet4Address) 0 else 1 }
                    } catch (e: UnknownHostException) {
                        throw e
                    }
                }
            })
            .addInterceptor { chain ->
                val ua = userAgents.random()
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", ua)
                        .header("Accept", "application/json, */*")
                        .header("Accept-Language", "ru-RU,ru;q=0.9")
                        .build()
                )
            }
            .build()
    }

    @Provides
    @Singleton
    fun provideWbApiService(client: OkHttpClient): WbApiService = WbApiService(client)
}

