package com.orbytum.api.service;

import com.orbytum.api.models.dto.response.LinkPreviewResponse;
import com.orbytum.api.models.exceptions.LinkPreviewIndisponivelErro;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Extração server-side de metadados de um link (REQ-079).
 *
 * A leitura da página externa é feita no back-end para evitar bloqueios de CORS
 * do navegador, retornando título e descrição obtidos das tags Open Graph/Twitter.
 */
@Service
public class LinkPreviewService {

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36";
    private static final int TIMEOUT_MS = 10_000;

    public LinkPreviewResponse extrair(String url) {
        URI uri = validarUrl(url);

        Document documento;
        try {
            documento = Jsoup.connect(uri.toString())
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MS)
                    .followRedirects(true)
                    .get();
        } catch (IOException | IllegalArgumentException ex) {
            throw new LinkPreviewIndisponivelErro(
                    "Não foi possível acessar o link informado. Verifique a URL ou preencha os campos manualmente.",
                    ex);
        }

        String titulo = primeiroNaoVazio(
                documento.select("meta[property=og:title]").attr("content"),
                documento.select("meta[name=twitter:title]").attr("content"),
                documento.title());

        String descricao = primeiroNaoVazio(
                documento.select("meta[property=og:description]").attr("content"),
                documento.select("meta[name=twitter:description]").attr("content"),
                documento.select("meta[name=description]").attr("content"));

        if (titulo == null && descricao == null) {
            throw new LinkPreviewIndisponivelErro(
                    "Não foi possível extrair título ou descrição do link. Preencha os campos manualmente.");
        }

        return new LinkPreviewResponse(uri.toString(), titulo, descricao);
    }

    private URI validarUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Informe o link da notícia.");
        }

        URI uri;
        try {
            uri = new URI(url.trim());
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("O link informado é inválido.");
        }

        String esquema = uri.getScheme();
        if (esquema == null || (!esquema.equalsIgnoreCase("http") && !esquema.equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException("O link deve começar com http:// ou https://");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank() || hostBloqueado(host)) {
            throw new IllegalArgumentException("O link informado é inválido.");
        }

        return uri;
    }

    private boolean hostBloqueado(String host) {
        String normalizado = host.toLowerCase();
        if (normalizado.equals("localhost") || normalizado.endsWith(".localhost")) {
            return true;
        }

        try {
            for (InetAddress endereco : InetAddress.getAllByName(normalizado)) {
                if (endereco.isLoopbackAddress()
                        || endereco.isAnyLocalAddress()
                        || endereco.isSiteLocalAddress()
                        || endereco.isLinkLocalAddress()) {
                    return true;
                }
            }
        } catch (IOException ignored) {
            // Host não resolvido: deixa o jsoup tentar e reportar a falha.
        }

        return false;
    }

    private String primeiroNaoVazio(String... valores) {
        for (String valor : valores) {
            if (valor != null && !valor.isBlank()) {
                return valor.trim();
            }
        }
        return null;
    }
}
