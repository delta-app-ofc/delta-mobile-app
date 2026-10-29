# Configurações de perfil — especificação de criação

## Escopo

Três telas Android nativas foram implementadas usando as referências `config-perfil.png`, `config-pessoal.png` e `config-residencial.png`. A interface e a edição residencial disponível na API estão preparadas; a edição pessoal e o envio de fotos dependem dos contratos de backend relacionados ao final deste documento.

Respeitar Java, XML, ViewModels/LiveData, repositórios, sessão autenticada, navegação e recursos existentes. Não reescrever o cadastro, a Home, o dashboard ou o formulário de conta de água.

**Regra principal:** todas as telas abrem em modo de visualização. Informações pessoais e residenciais só se tornam editáveis após tocar no lápis do canto superior direito. A tela principal não possui lápis global; a foto tem seu próprio ícone de edição.

Os nomes, endereços, telefones e o tempo de cadastro das imagens são exemplos visuais. Mostrar dados reais da conta/imóvel ou uma indicação de ausência, sem preencher com os exemplos.

## Base do projeto e pontos de integração

| Recurso existente | Orientação |
| --- | --- |
| `view/fragments/AccountSettingsFragment.java` e `accountConfigFragment` | Correspondem ao formulário de conta de água, apesar do nome. Preservar; não reutilizar esse destino como configurações de perfil. |
| `HomeFragment` e `DashboardFragment` | O ícone de configurações abre atualmente opções de imóvel, hidrômetro e saída. Direcionar o acesso à nova tela principal e preservar essas funções em locais apropriados do fluxo, sem eliminar o logout ou a seleção de imóvel. |
| `res/navigation/main_navigation.xml` | Adicionar destinos próprios para as três telas. Manter os destinos atuais e o funcionamento de Voltar. |
| `AuthRepository`, `ApiService`, `RetrofitClient`, `SessionInterceptor` | Reutilizar sessão e transporte autenticado da API Delta. Não criar outra sessão ou armazenar tokens no perfil. |
| `AuthModels.Profile` | Atualmente contém apenas `id`, `name` e `email`. Telefone, data de criação e foto exigem contrato confirmado e ampliação do modelo. |
| Endereço | `ApiService` já declara `getProperty`, `getAddress` e `updateAddress`. Resolver o endereço pelo imóvel autorizado; confirmar os campos realmente persistidos antes de habilitar edição. |
| CEP | Reutilizar `PostalCodeRepository` e a consulta BrasilAPI. O modelo `PostalCodeResponse` atual não declara bairro e rua; ampliar somente após confirmar a resposta real. |
| Menu e carregamento | Preservar o menu compartilhado e o carregamento de conteúdo que não bloqueia a navegação. Não introduzir diálogos de carregamento global nas novas telas. |
| Cloudinary | `.env` local possui `CLOUDINARY_URL` e `CLOUDINARY_FOLDER=delta-profile-picture-user`. O nome da pasta está configurado; isso não confirma sua criação remota nem a existência de uma API de upload. |

Os caminhos Java da tabela são relativos a `app/src/main/java/com/aula/delta_mobile_app/`.

## Aparência comum

- Fundo escuro, títulos e textos principais brancos, textos auxiliares cinza e ícones azuis, seguindo as imagens e os recursos existentes.
- Margens laterais aproximadas de 28–32dp; tamanhos e espaçamentos devem se adaptar à área útil do dispositivo. Não converter os pixels das imagens diretamente em coordenadas fixas.
- Voltar no alto à esquerda; títulos próximos de 24sp, com quebra natural em telas menores e fontes ampliadas.
- Campos com preenchimento cinza escuro, borda cinza clara fina, cantos arredondados e rótulo sobre a borda, conforme referências. Reutilizar Material/TextInputLayout se adequado, ajustando o estilo localmente.
- Rótulos e valores continuam legíveis em modo de visualização. Não aplicar a aparência esmaecida de um formulário inteiro desabilitado.
- Em visualização, campos não abrem o teclado nem aceitam digitação; permitir seleção/cópia de informações úteis, como e-mail e endereço.
- Usar strings, dimensões, cores e drawables em recursos. Reutilizar a fonte sans-serif do projeto, sem introduzir outra biblioteca visual apenas para essas telas.
- Área tocável mínima de 48dp para voltar, editar, salvar, cancelar e editar foto, ainda que o desenho do ícone seja menor.
- Usar conteúdo rolável, insets existentes e espaço para o menu. Evitar sobreposição dos últimos campos e botões com teclado, barras do sistema ou navegação flutuante.

## 1. Tela principal — Configurações

Referência: `config-perfil.png`.

### Cabeçalho e foto

- Exibir Voltar e o título “Configurações”.
- Abaixo, avatar circular de aproximadamente 68dp à esquerda, saudação “Olá, {primeiro nome}!” e tempo de cadastro quando houver data confiável.
- Se a data de criação não estiver disponível, omitir o tempo; não usar a data de vínculo ao imóvel como data de criação da conta.
- Avatar sem foto usa placeholder local. Falha ao carregar a imagem remota também usa placeholder e não impede acesso às opções.
- Sobrepor um lápis pequeno no canto inferior direito do avatar, com fundo circular contrastante e descrição “Alterar foto de perfil”. Somente esse controle inicia a troca de foto.
- Não colocar ícone global de edição no canto superior direito desta tela.

### Grupos de opções

Reproduzir os dois blocos arredondados da imagem, com ícones azuis, separadores discretos e seta à direita:

1. Informações pessoais.
2. Informações residenciais.
3. Unidades.
4. Sensores cadastrados.
5. Minha performance.

Os dois primeiros itens abrem as telas descritas abaixo. Os demais devem apontar para funcionalidades existentes quando corresponderem ao propósito do item; não usar o ranking como substituto automático de gerenciamento de unidades nem detecção de vazamento como lista de sensores. Quando não houver tela/contrato correspondente, indicar indisponibilidade explicitamente. Criar telas adicionais não faz parte destas três referências.

Manter uma ação de saída da conta acessível no fluxo de configurações, preservando a limpeza de sessão e dados já existente.

## 2. Informações pessoais

Referência: `config-pessoal.png`, com a alteração solicitada de um lápis global no canto superior direito.

- Exibir Voltar, ícone azul de pessoa e título “Informações pessoais”.
- Exibir campos Nome, E-mail, Senha e Telefone na ordem da imagem.
- Colocar o lápis global no alto à direita, sem sobrepor título ou área de Voltar. Descrição: “Editar informações pessoais”.
- Retirar o lápis isolado do campo Telefone mostrado na referência: a edição é controlada pelo lápis global.
- Nome, e-mail e telefone iniciam somente para leitura. Sem dado, mostrar “Não informado”. Esse texto é apresentação, não valor enviado à API.

### Senha

- A senha atual nunca deve ser consultada, recebida, preenchida ou revelada pelo aplicativo.
- Representar o campo com máscara fixa e descrição de senha protegida; a máscara não representa seu comprimento real.
- O ícone de olho da referência não deve revelar uma senha atual inexistente no cliente. Usar visibilidade somente nos campos de nova senha/confirmar senha dentro do fluxo de alteração.
- “Alterar senha” aparece conforme referência, mas só fica acionável no modo de edição. Deve abrir um fluxo separado, autenticado, com os campos e verificações que o backend exigir.
- Não enviar a máscara fixa como senha; não editar senha junto com nome, e-mail e telefone.
- Se o backend não oferecer alteração de senha, informar essa indisponibilidade ao tentar usar a ação, sem alegar sucesso.

### Edição e confirmação

- Ao tocar no lápis, criar rascunho a partir dos dados carregados e habilitar os campos suportados pelo contrato real.
- Apresentar “Salvar” e “Cancelar” de forma clara. Não transformar o lápis em um salvamento implícito sem indicação textual/acessível.
- Validar nome, formato de e-mail e telefone conforme regras já utilizadas no cadastro e exigências da API. Não exigir dados adicionais apenas porque constam da referência.
- Edição de e-mail pode exigir confirmação/reautenticação; seguir o contrato, mantendo o estado pendente visível quando aplicável.
- Se um campo não tiver atualização suportada, mantê-lo somente para leitura com explicação; não habilitar uma edição que será descartada silenciosamente.

## 3. Informações residenciais

Referência: `config-residencial.png`, com lápis global no canto superior direito.

- Exibir Voltar, ícone azul de casa e título “Informações residenciais”.
- Colocar o lápis global no alto à direita, com descrição “Editar informações residenciais”. Retirar o lápis isolado do CEP mostrado na referência.
- Primeira linha: CEP mais largo à esquerda e Estado à direita. Segunda: Cidade mais larga à esquerda e Número à direita. Em seguida, Bairro e Rua ocupando a largura disponível.
- Em telas estreitas ou fonte ampliada, empilhar campos em vez de cortar rótulos/valores.
- Todos os campos iniciam somente para leitura. O lápis habilita edição dos campos suportados e apresenta “Salvar” e “Cancelar”.
- Identificar claramente qual imóvel está sendo visualizado, principalmente quando houver múltiplas unidades. Usar a seleção existente e validar seu vínculo com a conta.

### CEP e persistência

- Consultar CEP apenas durante edição, depois de oito dígitos válidos, com debounce e descarte de respostas antigas quando o CEP mudar.
- A consulta sugere Estado, Cidade, Bairro e Rua somente quando esses campos forem retornados. Não inventar bairro/rua a partir da localização.
- Permitir corrigir campos retornados quando o contrato aceitar edição. Campo indisponível pela consulta não pode apagar um valor já existente automaticamente.
- O Número continua sob controle da pessoa; não deve ser substituído pela consulta de CEP.
- Não reutilizar o rascunho do cadastro como armazenamento destas alterações. Usar ViewModel próprio para não modificar um cadastro em andamento.
- `GET/PUT /delta/address/{id}` já existem no cliente, mas isso não garante suporte a Número, Bairro e Rua. Confirmar leitura/escrita desses campos e atualizar o backend quando necessário antes de habilitar seu salvamento.
- Enviar somente os dados previstos no contrato, preservando campos obrigatórios não exibidos. Confirmar regras de região tarifária e coordenadas caso sejam afetadas pela mudança de endereço.
- Se o endereço for compartilhado por mais de um imóvel, confirmar a regra de atualização da API para evitar alterar outros imóveis involuntariamente.

## Estados comuns de edição

| Estado | Comportamento |
| --- | --- |
| Carregando | Indicador no conteúdo, campos ainda não editáveis e navegação disponível. |
| Visualização | Dados confirmados, sem teclado automático; lápis disponível quando houver dados/autorização. |
| Edição | Rascunho separado dos dados confirmados, campos permitidos habilitados, Salvar e Cancelar visíveis. |
| Salvando | Bloquear nova gravação e mudanças no formulário; indicador local; não bloquear o menu inteiro. |
| Sucesso | Atualizar os dados confirmados, retornar à visualização e propagar alterações de nome/foto aos cabeçalhos existentes. |
| Falha | Manter o rascunho, mostrar erro e permitir nova tentativa; não apresentar alterações como persistidas. |
| Sem dados/sem imóvel | Estado vazio explícito; não gerar IDs ou registros fictícios. |
| Sessão encerrada | Cancelar operações quando possível, descartar callbacks antigos e limpar dados pessoais em memória. |

- Cancelar abandona o rascunho e restaura os últimos dados confirmados.
- Voltar ou trocar de aba com alterações não salvas deve oferecer continuar editando ou descartar. Sem alterações, navegar imediatamente.
- Preservar rascunho não sensível na recriação da view; não salvar senhas em Bundle/SavedStateHandle, disco ou logs.
- Caso a pessoa navegue durante uma gravação, a conclusão pertence à conta e ao registro originais; não aplicar dados a uma nova seleção de imóvel ou sessão.
- Não repetir gravações automaticamente por recriação de tela. Se houver conflito concorrente, recarregar ou seguir controle de versão oferecido pela API.

## Foto de perfil — câmera, galeria e Cloudinary

### Seleção

1. Tocar no lápis do avatar abre as opções “Tirar foto”, “Escolher da galeria” e “Cancelar”.
2. “Tirar foto” usa Activity Result API com `TakePicture` e URI `content://` via FileProvider para arquivo temporário. Configurar provider não exportado e os caminhos mínimos necessários no Manifest/XML.
3. O projeto já declara permissão de câmera por causa do leitor QR. Tratar a permissão necessária ao fluxo escolhido, recusa e ausência de aplicativo de câmera sem afetar a galeria.
4. “Escolher da galeria” usa `PickVisualMedia` limitado a uma imagem. Usar o fallback oferecido pelo contrato em dispositivos sem Photo Picker; não exigir acesso irrestrito ao armazenamento.
5. Cancelar a seleção/captura mantém a foto atual, sem upload nem alteração do perfil.
6. Validar imagem selecionada, aplicar orientação e apresentar prévia circular antes de confirmar. Recorte manual é opcional; não exigir biblioteca de recorte sem necessidade.

Usar limites explícitos compartilhados com o backend. Proposta inicial: imagem estática de até 10MB, JPEG/PNG/WebP, redimensionada para no máximo 1024px no lado maior, preservando proporção e qualidade. Confirmar esses limites antes da implementação e tratar HEIC/formatos não suportados com conversão suportada ou erro claro. Remover metadados de localização da cópia enviada; não sobrescrever o arquivo original.

### Upload autenticado e associação ao perfil

**A URL completa do `.env` contém segredo de API. Deve ser consumida pelo backend, nunca incluída em BuildConfig, recursos, assets ou APK.** O `.env` atual está ignorado pelo Git; isso não configura automaticamente um servidor nem uma integração Android.

Fluxo recomendado:

1. O app, autenticado na API Delta, solicita uma autorização de upload ao backend para sua própria foto.
2. O backend determina o usuário, o destino e os parâmetros permitidos e gera assinatura temporária com o segredo Cloudinary. Não permitir assinatura de parâmetros arbitrários enviados pelo cliente.
3. O app envia o arquivo ao endpoint de upload do Cloudinary usando os parâmetros assinados; não encaminha o token Delta ao domínio Cloudinary.
4. O backend confirma o ativo enviado e associa sua identificação/URL ao perfil autenticado. Uma alternativa válida é o backend receber o arquivo e realizar o upload, mantendo a mesma confirmação de persistência.
5. Só depois da associação confirmada, mostrar a nova foto como salva e atualizar Home, dashboard e configurações. A prévia antes disso deve ser identificada como não salva.

Usar o destino lógico `delta-profile-picture-user`, conforme `CLOUDINARY_FOLDER`. Confirmar o modo da conta Cloudinary: pasta fixa usa parâmetros de pasta correspondentes; pastas dinâmicas usam `asset_folder`. Não presumir que um prefixo em `public_id` movimenta o ativo para uma pasta dinâmica.

- Persistir no backend `secure_url` e identificadores necessários à gestão do ativo, como `public_id`/`asset_id`, conforme contrato. Não usar o caminho temporário do Android como URL do perfil.
- Atualização de foto deve invalidar o cache pela URL/versão retornada; evitar continuar exibindo a imagem antiga após sucesso.
- Manter a foto anterior até confirmar a atualização do perfil. Se o upload funcionar e a associação falhar, manter o estado recuperável e reconciliar/remover o novo ativo órfão pelo backend.
- Só remover/substituir o ativo anterior conforme regra do backend e depois da confirmação; não destruir ativos de outro usuário por IDs fornecidos pelo app.
- Progresso no avatar ou próximo à prévia, repetição manual em falha e prevenção de uploads duplicados. Não bloquear todo o menu durante upload.
- Cancelamento e troca de conta não devem aplicar uma foto à conta errada. O backend precisa validar vínculo/autorização em cada etapa.
- Limpar arquivos temporários e tratar perda de acesso à URI na recriação/processo. Copiar para cache privado quando necessário para concluir uma operação autorizada.

## Contratos que precisam ser confirmados

Não inventar URLs de endpoints. Os seguintes contratos não foram encontrados no cliente atual e devem ser definidos/confirmados antes de uma implementação completa:

| Necessidade | Contrato mínimo |
| --- | --- |
| Perfil completo | Ler nome, e-mail, telefone, data de criação e foto da conta autenticada. |
| Atualização pessoal | Alterar os campos permitidos, com validação, autorização e regras para mudança de e-mail. |
| Senha | Fluxo separado, verificações exigidas, resposta e eventual invalidação de sessão. |
| Upload de foto | Autorização/assinatura ou proxy autenticado, formatos/limites e erros. |
| Associação da foto | Confirmar ativo do usuário, persistir URL/identificação e consultar a foto após novo login. |
| Endereço completo | Confirmar Número/Bairro/Rua, ownership do imóvel/endereço e payload seguro de atualização. |

Interfaces podem ser preparadas enquanto contratos faltam, mas ações dependentes devem indicar indisponibilidade. Nunca simular upload concluído ou edição salva apenas em memória.

## Organização sugerida da implementação

- `ProfileSettingsFragment` / `fragment_profile_settings.xml`: tela principal.
- `PersonalInformationFragment` / `fragment_personal_information.xml`: dados pessoais e modo de edição.
- `ResidentialInformationFragment` / `fragment_residential_information.xml`: endereço do imóvel selecionado.
- ViewModels próprios para estado confirmado, rascunho, validação e operações; repositório de perfil e repositório de upload, preservando os padrões atuais.
- Recursos locais `profile_strings.xml`, `profile_styles.xml`, dimensões e ícones vetoriais, reutilizando os tokens já adequados.
- Destinos novos em `main_navigation.xml` e integração com os ícones de configurações da Home/dashboard. Não substituir `accountConfigFragment`.
- Carregamento de imagem remota com cache, placeholder e ciclo de vida correto. Verificar primeiro as dependências disponíveis antes de adicionar uma biblioteca.

Seguir o README: XML com quatro espaços e um atributo por linha em elementos com vários atributos; Java compacto e sem comentários no código.

## Critérios de aceite

- [ ] As três telas seguem as referências com dados reais e estados vazios explícitos.
- [ ] Principal sem lápis global; avatar com edição própria; pessoais e residenciais com lápis no canto superior direito.
- [ ] Campos iniciam somente para leitura, preservando contraste; lápis ativa edição; Salvar/Cancelar têm comportamento verificável.
- [ ] Cancelar e navegar com rascunho não provocam gravações acidentais.
- [ ] Senha atual não é recuperada, revelada, armazenada ou enviada como máscara.
- [ ] Câmera e galeria funcionam com sucesso, cancelamento, recusa de permissão, arquivo inválido e dispositivo sem câmera.
- [ ] Foto é enviada ao Cloudinary no destino configurado e vinculada à conta pelo backend; persiste após novo login.
- [ ] Falha entre upload e associação não perde a foto antiga nem apresenta sucesso falso.
- [ ] Nenhum segredo Cloudinary vai para o Git, logs, BuildConfig ou APK.
- [ ] Endereço pertence ao imóvel autorizado; campos não suportados não aparentam ter sido salvos.
- [ ] Troca de imóvel/conta e callbacks antigos não misturam dados ou imagens.
- [ ] Menu continua utilizável durante carregamentos; teclado, rotação, fonte ampliada e telas pequenas não cortam campos ou botões.
- [ ] Formulário de conta de água, seleção de imóvel, logout e demais destinos existentes continuam funcionando.
- [ ] Executar testes relevantes de validação/estado/contrato, build e lint. Registrar bloqueios reais sem declarar validação visual ou ponta a ponta que não ocorreu.

## Referências técnicas consultadas

- [Android Photo Picker](https://developer.android.com/training/data-storage/shared/photo-picker): seleção limitada à mídia escolhida e fallback do contrato.
- [Android TakePicture](https://developer.android.com/reference/androidx/activity/result/contract/ActivityResultContracts.TakePicture): captura para a URI fornecida.
- [Cloudinary — uploads autenticados](https://cloudinary.com/documentation/upload_images#authenticated_requests): assinatura gerada no servidor e parâmetros de upload.

## Implementação local — 25/10/2026

### Entregue no aplicativo

- `ProfileSettingsFragment`, `PersonalInformationFragment` e `ResidentialInformationFragment`, com XMLs próprios em `res/layout`, estilos/textos/ícones locais e `ProfileFormField` para campos delineados legíveis em modo de leitura.
- `ProfileSettingsViewModel`, `ProfileSettingsRepository`, `ProfilePhotoRepository`, `ProfileSettingsData` e `ResidentialDraft` nas pastas existentes correspondentes. Os dados e rascunhos não usam o fluxo de cadastro.
- As engrenagens da Home e do dashboard passam a abrir `profileSettingsFragment`. O restante das telas mantém sua implementação. Seleção de imóvel e logout estão acessíveis nas novas configurações; o formulário de conta de água permanece em seu destino original.
- Principal com placeholder circular, lápis de foto, saudação pelo nome real, dois grupos de opções e estados explícitos para funções sem tela disponível. Nenhum tempo de cadastro é inventado.
- Informações pessoais consultadas por `/delta/auth/me`. O lápis explica a indisponibilidade de atualização pessoal; os campos permanecem somente para leitura porque não há rota de gravação. A senha é uma máscara fixa não revelável. Telefone ausente aparece como não informado.
- Residência resolvida pelo imóvel selecionado, com validação do vínculo do usuário. O lápis habilita CEP, cidade e estado; Salvar usa o PUT existente e Cancelar restaura os dados confirmados. Número, bairro e rua permanecem indisponíveis, sem simular persistência.
- Consulta de CEP com debounce e descarte de respostas antigas; edições manuais em cidade/estado durante uma consulta são preservadas. Região tarifária é validada pelo padrão existente. Coordenadas de um CEP anterior não são enviadas como se fossem do novo endereço.
- Antes de salvar, o repositório consulta o endereço novamente para detectar divergência em relação ao snapshot. Isso reduz sobrescrita acidental, mas não substitui controle atômico de concorrência no backend. A resposta de gravação precisa confirmar campos, região e coordenadas solicitados.
- `ProfileNavigationGuard` atua apenas nos novos destinos, incluindo os toques no menu, para confirmar descarte de alterações não salvas. Uma gravação já iniciada continua vinculada ao registro original.
- Foto por `TakePicture`/FileProvider e Photo Picker, com tratamento de cancelamento/recusa e cache privado. Preparação fora da thread da interface: limite local de 10 MB, normalização para JPEG de até 1024px, orientação aplicada pelo decodificador e sem copiar metadados EXIF. Há prévia circular e descarte; a imagem ainda não é tratada como salva no perfil.
- O botão “Enviar foto” informa a dependência de autorização/associação pelo backend. Não realiza upload não autorizado, não inclui o segredo do `.env` no APK e não simula sucesso.

### Backend ainda necessário

O contrato publicado em `https://delta-api-postgres.onrender.com/v3/api-docs` foi consultado nesta implementação. Ele confirma apenas `id`, `name` e `email` em `UserInfo`; endereço contém `id`, `regionId`, `cep`, `city`, `state`, `latitude` e `longitude`.

Para concluir os fluxos que ainda aparecem indisponíveis, o backend deve oferecer:

1. Leitura e atualização autorizada do perfil completo, incluindo telefone e referência da foto.
2. Alteração de senha com as verificações definidas pelo produto.
3. Autorização de upload assinado no Cloudinary ou upload por proxy autenticado.
4. Associação e consulta da foto no perfil, gestão do ativo anterior e reconciliação em falhas.
5. Leitura/gravação de número, bairro e rua, se esses campos forem mantidos no produto.

O backend deverá consumir `CLOUDINARY_URL` e o destino `CLOUDINARY_FOLDER=delta-profile-picture-user`; o `.env` local deste repositório Android não configura o servidor remoto. Nenhuma rota inexistente foi adicionada ao cliente para aparentar suporte a essas operações.

### Verificação

- Recursos das novas telas compilados e ligados com AAPT2 e todas as fontes Java do aplicativo compiladas diretamente.
- Quatro testes de `ResidentialDraftTest` cobrem isolamento de rascunho, limite/validação dos campos, preservação/descarte de coordenadas e formato do payload suportado pela API.
- Dez testes existentes de conversa/contrato também passaram durante a compilação de integração.
- `gradlew.bat testDebugUnitTest assembleDebug lintDebug` foi tentado, mas continua falhando com `Unable to establish loopback connection`. A compilação direta não substitui o build e o lint completos.
- Não foi instalado um novo APK nem executada validação visual, de câmera/galeria ou de atualização autenticada no aparelho. Os critérios correspondentes permanecem pendentes; nenhum dado real foi alterado no servidor nesta implementação.
