Pod::Spec.new do |s|
  s.name         = 'UMCCommonLog'
  s.version      = '2.0.2'
  s.summary      = 'UMeng Common Log SDK (vendored)'
  s.description  = 'Vendored local copy of UMCCommonLog 2.0.2 for Xcode Cloud compatibility.'
  s.homepage     = 'https://developer.umeng.com'
  s.license      = { :type => 'Commercial' }
  s.author       = { 'Umeng' => 'support@umeng.com' }
  s.platform     = :ios, '12.0'
  s.source       = { :path => '.' }
  s.vendored_frameworks = 'UMCommonLog/UMCommonLog.framework'
  s.resource     = 'UMCommonLog/UMCommonLog.bundle'
end
