# Segurança

O Home Control controla a casa de pessoas reais. Se você achou uma falha de segurança (por exemplo, algo que deixe alguém de fora acionar a casa, ler o token ou instalar um APK falso como atualização), **avise em particular**:

- [Abra um aviso privado de segurança](https://github.com/duduferaso/ha_local_assistant/security/advisories/new) no GitHub.
- Não abra issue pública e não poste detalhes antes da correção.
- **Nunca mande o seu token do Home Assistant**, nem em particular. Se ele vazou, revogue em Perfil → Segurança → Tokens de acesso de longa duração no seu Home Assistant.

Responderei assim que possível e combinamos juntos quando divulgar.

## O que o app faz para proteger a casa

- O áudio só vai para o Home Assistant da casa. Nenhuma telemetria.
- O token fica guardado criptografado no celular e só entra num pacote de configuração se a pessoa escolher "com token".
- Atualizações só instalam se forem assinadas pela chave do mantenedor e o SHA-256 conferir.
- O usuário do Home Assistant recomendado é sem administrador.
