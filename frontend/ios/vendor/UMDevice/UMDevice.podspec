Pod::Spec.new do |s|
  s.name         = 'UMDevice'
  s.version      = '3.5.0'
  s.summary      = 'UMeng Device SDK (vendored)'
  s.description  = 'Vendored local copy of UMDevice 3.5.0 for Xcode Cloud compatibility.'
  s.homepage     = 'https://developer.umeng.com'
  s.license      = { :type => 'Commercial' }
  s.author       = { 'Umeng' => 'support@umeng.com' }
  s.platform     = :ios, '12.0'
  s.source       = { :path => '.' }
  s.vendored_frameworks = 'UMDevice_3.5.0/UMDevice.xcframework'
  s.frameworks   = 'CoreTelephony', 'SystemConfiguration', 'CoreLocation'
end
