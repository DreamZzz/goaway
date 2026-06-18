Pod::Spec.new do |s|
  s.name         = 'UMCommon'
  s.version      = '7.5.10'
  s.summary      = 'UMeng Common SDK (vendored)'
  s.description  = 'Vendored local copy of UMCommon 7.5.10 for Xcode Cloud compatibility.'
  s.homepage     = 'https://developer.umeng.com'
  s.license      = { :type => 'Commercial' }
  s.author       = { 'Umeng' => 'support@umeng.com' }
  s.platform     = :ios, '12.0'
  s.source       = { :path => '.' }
  s.vendored_frameworks = 'UMCommon_7.5.10/UMCommon.xcframework'
  s.dependency 'UMDevice'
  s.libraries  = 'sqlite3', 'z'
  s.frameworks = 'CoreTelephony', 'SystemConfiguration'
end
