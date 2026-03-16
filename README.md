# Net Watchman 🛡️

O **Net Watchman** é uma ferramenta robusta e moderna para monitoramento de dispositivos em redes locais (LAN). Desenvolvido para Android, o aplicativo permite que administradores de rede e entusiastas acompanhem a disponibilidade, latência e uptime de seus servidores, roteadores, câmeras e dispositivos IoT em tempo real.

## 🚀 Principais Funcionalidades

*   **Monitoramento em Tempo Real:** Verificação automática de status (Online/Offline) com suporte a ping ICMP e fallback para portas comuns.
*   **Gráficos de Latência:** Visualização intuitiva das flutuações de latência (ms) dos últimos eventos de cada dispositivo.
*   **Relatórios em PDF:** Geração de relatórios profissionais detalhados, incluindo estatísticas de uptime e histórico por dispositivo.
*   **Escaneamento de Rede Inteligente:** Detecção automática da subrede local e busca rápida de dispositivos ativos.
*   **Notificações Críticas:** Alertas imediatos caso um dispositivo monitorado fique offline.
*   **Wake-on-LAN (WOL):** Ligue computadores remotamente enviando pacotes mágicos.
*   **Gestão por Categorias:** Organize seus dispositivos (Servidores, Câmeras, IOT, etc.) com filtros rápidos.
*   **Backup e Importação:** Segurança total dos dados com exportação e importação via JSON.

## 🛠️ Tecnologias Utilizadas

*   **Linguagem:** [Kotlin](https://kotlinlang.org/)
*   **Arquitetura:** MVVM (Model-View-ViewModel) com Clean Code.
*   **Persistência:** [Room Database](https://developer.android.com/training/data-storage/room) para armazenamento local offline.
*   **Assincronia:** [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) & [Flow](https://kotlinlang.org/docs/flow.html) para UI reativa.
*   **Background Task:** [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) para monitoramento periódico em segundo plano.
*   **UI/UX:** [Material Design 3](https://m3.material.io/) com suporte a Edge-to-Edge e Dark Mode.
*   **Relatórios:** Geração nativa de PDF com `PdfDocument`.

## 📸 Screenshots

*(Espaço reservado para imagens do seu app)*

## 📦 Como Instalar

1.  Clone este repositório:
    ```bash
    git clone https://github.com/SEU_USUARIO/net-watchman.git
    ```
2.  Abra o projeto no **Android Studio (Hedgehog ou superior)**.
3.  Sincronize o Gradle e execute no seu dispositivo ou emulador.

## 📄 Licença

Este projeto está sob a licença MIT. Consulte o arquivo [LICENSE](LICENSE) para mais detalhes.

---
Desenvolvido por [Seu Nome/Nick] com ❤️.
