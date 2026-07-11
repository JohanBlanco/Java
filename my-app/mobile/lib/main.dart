import 'package:flutter/material.dart';

import 'package:provider/provider.dart';

import 'providers/auth_provider.dart';

import 'providers/theme_provider.dart';

import 'providers/layout_provider.dart';

import 'screens/login_screen.dart';

import 'screens/home_screen.dart';

import 'screens/platform_screen.dart';



void main() {

  runApp(const GymPlatformApp());

}



class GymPlatformApp extends StatelessWidget {

  const GymPlatformApp({super.key});



  @override

  Widget build(BuildContext context) {

    return MultiProvider(

      providers: [

        ChangeNotifierProvider(create: (_) => ThemeProvider()..load()),

        ChangeNotifierProvider(create: (_) => LayoutProvider()..load()),

        ChangeNotifierProvider(create: (_) => AuthProvider()..loadStoredAuth()),

      ],

      child: Consumer<ThemeProvider>(

        builder: (context, themeProvider, _) {

          if (!themeProvider.isLoaded) {

            return MaterialApp(

              debugShowCheckedModeBanner: false,

              theme: AppThemes.light(),

              darkTheme: AppThemes.dark(),

              themeMode: ThemeMode.dark,

              home: const Scaffold(

                body: Center(child: CircularProgressIndicator()),

              ),

            );

          }



          return MaterialApp(

            title: 'GymPlatform',

            debugShowCheckedModeBanner: false,

            theme: AppThemes.light(),

            darkTheme: AppThemes.dark(),

            themeMode: themeProvider.mode,

            home: const AuthGate(),

          );

        },

      ),

    );

  }

}



class AuthGate extends StatelessWidget {

  const AuthGate({super.key});



  @override

  Widget build(BuildContext context) {

    final auth = context.watch<AuthProvider>();



    if (auth.isLoading) {

      return const Scaffold(

        body: Center(child: CircularProgressIndicator()),

      );

    }



    if (!auth.isAuthenticated) {

      return const LoginScreen();

    }



    if (auth.hasRole('PLATFORM_OWNER')) {

      return const PlatformScreen();

    }



    return const HomeScreen();

  }

}


