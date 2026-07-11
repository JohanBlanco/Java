# Publicar la Wiki en GitHub

Las páginas están en `docs/wiki/`. GitHub Wiki es un **repositorio git separado**.

## Opción A — Desde la web de GitHub

1. Ve a tu repo en GitHub → pestaña **Wiki**
2. Si no existe, click **Create the first page**
3. Copia el contenido de cada archivo de `docs/wiki/`:
   - `Home.md` → página principal
   - `Getting-Started.md`, `Testing-Guide.md`, etc.
4. Crea `_Sidebar.md` para el menú lateral

## Opción B — Clonar el repo wiki (recomendado)

```bash
# Reemplaza USER y REPO con tu repositorio
git clone https://github.com/USER/REPO.wiki.git
cd REPO.wiki

# Copiar páginas desde el proyecto
cp ../my-app/docs/wiki/*.md .

git add .
git commit -m "docs: initial GymPlatform wiki"
git push
```

## Sincronizar cambios futuros

Cuando actualices documentación:

1. Edita archivos en `docs/wiki/` del proyecto principal
2. Copia al repo `.wiki.git`
3. Commit y push al wiki

O automatiza con un script:

```bash
#!/bin/bash
# scripts/sync-wiki.sh
WIKI_DIR="../REPO.wiki"
cp docs/wiki/*.md "$WIKI_DIR/"
cd "$WIKI_DIR" && git add . && git commit -m "docs: sync from main repo" && git push
```

## Sidebar

El archivo `_Sidebar.md` genera el menú lateral automáticamente en GitHub Wiki.

## Enlazar desde el README

Agrega en el README del repo principal:

```markdown
📖 [Documentación completa en Wiki](https://github.com/USER/REPO/wiki)
```

## Notas

- Las imágenes en wiki requieren subirlas al repo wiki o usar URLs externas
- GitHub Wiki no soporta todos los features de Markdown (ej. algunos HTML)
- Mantén `docs/wiki/` en el repo principal como fuente de verdad; el wiki es el espejo publicado
