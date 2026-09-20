# Версия и идентификатор сборки — единственное место, где они задаются.
#
# VERSION совпадает с android:versionName в AndroidManifest.xml; build.sh это
# проверяет.
VERSION="0.7-live-test"

# BUILD_TAG делает имя файла уникальным для каждой сборки.
#
# AppManager на ГУ не показывает .epk, если файл с таким именем уже
# попадался: пользователю приходилось переименовывать пакет руками перед
# каждой поездкой. Уникальное имя снимает это совсем.
#
# В CI берётся номер прогона — он монотонно растёт и сразу говорит, какая это
# сборка. Локально берётся дата и время.
if [ -n "${GITHUB_RUN_NUMBER:-}" ]; then
    BUILD_TAG="b${GITHUB_RUN_NUMBER}"
else
    BUILD_TAG="$(date -u +%m%d-%H%M)"
fi

# Полное имя без расширения: его ждут build.sh, build-epk.sh и provenance.sh.
ARTIFACT_BASE="Q50-GTR-Plus-v${VERSION}-${BUILD_TAG}"
