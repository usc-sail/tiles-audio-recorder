# Jelly openSMILE Pilot 

### - NOT FOR COMMERCIAL USE - Library credit to openSMILE team

================
### Setup

#### Step1: Download Android Studio, openSMILE and Android NDK

#### Step2: Follow openSMILE handbook to setup openSMILE lib folder and NDK folder location in project files

#### Step3: Update your own Amazon keys in Constant.Java if you want to transmit your feature files to your Amazon S3 server

#### Step4: Hit run in Android studio and hope all is good

The feature files will be extracted every 5 minutes for a duration of 10 seconds in csv format. The csv format files will be transmit to your Amazon S3 server.


openSMILE
   - open-Source Media Interpretation by Large feature-space Extraction -
  Copyright (C) 2008-2013  Florian Eyben, Felix Weninger, Martin Woellmer, Bjoern Schuller, TUM MMK
  Copyright (C) 2013-2014 audEERING UG (limited)

  audEERING UG (limited)
  Gilching, Germany

 ********************************************************************** 
 If you use openSMILE or any code from openSMILE in your research work,
 you are kindly asked to acknowledge the use of openSMILE in your publications.
 See the file CITING.txt for details.
 **********************************************************************

  This copy of openSMILE is distributed under the terms of a research only license,
  which does not allow commercial use. For details, please see the file COPYING.
 
 ++ For commercial licensing options, please contact the info@audeering.com ++
 
 About openSMILE:
================

openSMILE is a complete and open-source toolkit for audio analysis, processing and classification especially targeted at speech and music applications, e.g. ASR, emotion recognition, or beat tracking and chord detection.
The toolkit was developed at the Institute for Human-Machine Communication at the Technische Universitaet Muenchen in Munich, Germany and is now maintained
and developed further by audEERING UG (limited). 
It was started initally for the SEMAINE EU FP7 project.


Third-party dependencies:
=========================

openSMILE uses LibSVM (by Chih-Chung Chang and Chih-Jen Lin) for classification tasks. It is distributed with openSMILE and is included in the svm/ directory.

PortAudio is required for live recording from sound card and for the SEMAINE component.
You can get it from: http://www.portaudio.com
A working snapshot is included in thirdparty/portaudio.tgz

Optionally, openSMILE can be linked against the SEMAINE API and the Julius LVCSR engine, enabling an interface to the SEMAINE system and a keyword spotter component. See http://www.semaine-project.eu/ for details on running the SEMAINE system. Note, that the SEMAINE API is only offically supported until Version 1.0.1 (the last GPL release), but nobody will complain if you link openSMILE 2.x against the SEMAINE API for your private or educational use.


Documentation/Installing/Using:
===============================

openSMILE is well documented in the openSMILE book, which can be 
found in doc/openSMILE_book.pdf.

For quick-start information on how to compile openSMILE, see the file INSTALL.

Developers:
===========

Incomplete developer's documentation can be found in "doc/developer" 
and in the openSMILE book.

Information on how to write and compile run-time linkable plug-ins 
for openSMILE, see the openSMILE book or take a look at the files 
in the "plugindev" directory, especially the README file.

 
