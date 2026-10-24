# Início — launcher leve para Android TV

Launcher próprio em Kotlin + Compose for TV: grade só com os apps favoritos, atalhos para as entradas HDMI, sem anúncios.

## Status
O código está pronto, mas **ainda não foi compilado nem testado**. O primeiro build pode precisar de algum ajuste.

## Como compilar (GitHub Actions)
1. Crie um repositório no GitHub e envie esta pasta inteira para a branch `main`.
2. O workflow `.github/workflows/build.yml` roda sozinho e publica o APK numa Release.
3. No celular, baixe a versão mais nova pelo link:
   `https://github.com/<usuario>/<repo>/releases/latest/download/tv-launcher.apk`

## Como instalar na TV
1. Conecte o celular à TV via ADB (Bugjaeger, `IP-da-TV:5555`).
2. Instale o `tv-launcher.apk` pelo app.
3. Aperte Home no controle e escolha **Início** como tela inicial.

## Uso
- **OK** abre o app selecionado.
- **Segurar OK** num favorito abre o menu para mover ou remover.
- Em **Todos os apps**, segurar OK adiciona ou tira o app dos favoritos (★).

## Observação
A pasta `keystore/` tem a chave que assina o APK, para que as atualizações instalem por cima da versão anterior. Se o repositório for público, é melhor movê-la para os Secrets do GitHub.
