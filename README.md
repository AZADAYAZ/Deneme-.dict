# AOSP Dictionary Editor

Bu proje, Android AOSP klavyeleri tarafından kullanılan `.dict` (ikili) sözlük dosyalarını düzenlemenize olanak tanır.

## Özellikler
- `.dict` dosyalarını içe aktarma ve kelime listesini çözme.
- Kelime ekleme, silme ve frekans (puan) düzenleme.
- Düzenlenen sözlüğü tekrar `.dict` formatında dışa aktarma.
- GitHub Actions ile otomatik APK oluşturma.

## Nasıl Kullanılır?
1. Bu projeyi GitHub hesabınıza yükleyin.
2. GitHub Actions sekmesinde "Android CI" workflow'unun tamamlanmasını bekleyin.
3. Oluşturulan `app-debug.apk` dosyasını telefonunuza indirin ve kurun.
4. Uygulama içinden düzenlemek istediğiniz `.dict` dosyasını seçin.

## Teknik Detaylar
Uygulama, AOSP Binary Dictionary v2 formatını temel alan bir Trie yapısı kullanır. Karmaşık v4 sözlükleri için `dicttool_aosp.jar` aracını kullanmanız önerilir.

## Lisans
MIT
