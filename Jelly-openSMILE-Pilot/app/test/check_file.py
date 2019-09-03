import sys
from glob import glob
import pandas as pd
from datetime import datetime

cols = ["file_path", "time_UTC", "unixtime", "file_check"]


# these are the file header in the order

feat_names = ['frameIndex', 'frameTime', 'F0final_sma', 'voicingFinalUnclipped_sma', 'jitterLocal_sma', 'jitterDDP_sma',
              'shimmerLocal_sma', 'logHNR_sma', 'voiceProb_sma', 'F0_sma', 'F0env_sma', 'audspec_lengthL1norm_sma',
              'audspecRasta_lengthL1norm_sma', 'pcm_RMSenergy_sma', 'pcm_zcr_sma', 'pcm_intensity_sma',
              'pcm_loudness_sma', 'audSpec_Rfilt_sma[0]', 'audSpec_Rfilt_sma[1]', 'audSpec_Rfilt_sma[2]',
              'audSpec_Rfilt_sma[3]', 'audSpec_Rfilt_sma[4]', 'audSpec_Rfilt_sma[5]', 'audSpec_Rfilt_sma[6]',
              'audSpec_Rfilt_sma[7]', 'audSpec_Rfilt_sma[8]', 'audSpec_Rfilt_sma[9]', 'audSpec_Rfilt_sma[10]',
              'audSpec_Rfilt_sma[11]', 'audSpec_Rfilt_sma[12]', 'audSpec_Rfilt_sma[13]', 'audSpec_Rfilt_sma[14]',
              'audSpec_Rfilt_sma[15]', 'audSpec_Rfilt_sma[16]', 'audSpec_Rfilt_sma[17]', 'audSpec_Rfilt_sma[18]',
              'audSpec_Rfilt_sma[19]', 'audSpec_Rfilt_sma[20]', 'audSpec_Rfilt_sma[21]', 'audSpec_Rfilt_sma[22]',
              'audSpec_Rfilt_sma[23]', 'audSpec_Rfilt_sma[24]', 'audSpec_Rfilt_sma[25]', 'pcm_fftMag_fband250-650_sma',
              'pcm_fftMag_fband1000-4000_sma', 'pcm_fftMag_spectralRollOff25.0_sma',
              'pcm_fftMag_spectralRollOff50.0_sma', 'pcm_fftMag_spectralRollOff75.0_sma',
              'pcm_fftMag_spectralRollOff90.0_sma', 'pcm_fftMag_spectralFlux_sma', 'pcm_fftMag_spectralCentroid_sma',
              'pcm_fftMag_spectralEntropy_sma', 'pcm_fftMag_spectralVariance_sma', 'pcm_fftMag_spectralSkewness_sma',
              'pcm_fftMag_spectralKurtosis_sma', 'pcm_fftMag_spectralSlope_sma', 'pcm_fftMag_psySharpness_sma',
              'pcm_fftMag_spectralHarmonicity_sma', 'lpcCoeff_sma[0]', 'lpcCoeff_sma[1]', 'lpcCoeff_sma[2]',
              'lpcCoeff_sma[3]', 'lpcCoeff_sma[4]', 'lpcCoeff_sma[5]', 'lpcCoeff_sma[6]', 'lpcCoeff_sma[7]',
              'mfcc_sma[1]', 'mfcc_sma[2]', 'mfcc_sma[3]', 'mfcc_sma[4]', 'mfcc_sma[5]', 'mfcc_sma[6]', 'mfcc_sma[7]',
              'mfcc_sma[8]', 'mfcc_sma[9]', 'mfcc_sma[10]', 'mfcc_sma[11]', 'mfcc_sma[12]', 'mfcc_sma[13]',
              'mfcc_sma[14]', 'F0final_sma_ff0', 'voiceProb_sma_de', 'F0_sma_de', 'F0env_sma_de',
              'audspec_lengthL1norm_sma_de', 'audspecRasta_lengthL1norm_sma_de', 'pcm_RMSenergy_sma_de',
              'pcm_zcr_sma_de', 'pcm_intensity_sma_de', 'pcm_loudness_sma_de', 'mfcc_sma_de[1]', 'mfcc_sma_de[2]',
              'mfcc_sma_de[3]', 'mfcc_sma_de[4]', 'mfcc_sma_de[5]', 'mfcc_sma_de[6]', 'mfcc_sma_de[7]',
              'mfcc_sma_de[8]', 'mfcc_sma_de[9]', 'mfcc_sma_de[10]', 'mfcc_sma_de[11]', 'mfcc_sma_de[12]',
              'mfcc_sma_de[13]', 'mfcc_sma_de[14]', 'voiceProb_sma_de_de', 'F0_sma_de_de', 'F0env_sma_de_de',
              'audspec_lengthL1norm_sma_de_de', 'audspecRasta_lengthL1norm_sma_de_de', 'pcm_RMSenergy_sma_de_de',
              'pcm_zcr_sma_de_de', 'pcm_intensity_sma_de_de', 'pcm_loudness_sma_de_de', 'mfcc_sma_de_de[1]',
              'mfcc_sma_de_de[2]', 'mfcc_sma_de_de[3]', 'mfcc_sma_de_de[4]', 'mfcc_sma_de_de[5]', 'mfcc_sma_de_de[6]',
              'mfcc_sma_de_de[7]', 'mfcc_sma_de_de[8]', 'mfcc_sma_de_de[9]', 'mfcc_sma_de_de[10]', 'mfcc_sma_de_de[11]',
              'mfcc_sma_de_de[12]', 'mfcc_sma_de_de[13]', 'mfcc_sma_de_de[14]', 'isTurn']


emobase_feat_names = ['frameIndex','frameTime','voiceProb_sma','F1_sma','F0env_sma','audspec_lengthL1norm_sma','audspecRasta_lengthL1norm_sma','pcm_RMSenergy_sma','pcm_zcr_sma','pcm_intensity_sma','pcm_loudness_sma','pcm_fftMag_fband250-650_sma','pcm_fftMag_fband1000-4000_sma','pcm_fftMag_spectralRollOff25.0_sma','pcm_fftMag_spectralRollOff50.0_sma','pcm_fftMag_spectralRollOff75.0_sma','pcm_fftMag_spectralRollOff90.0_sma','pcm_fftMag_spectralFlux_sma','pcm_fftMag_spectralCentroid_sma','pcm_fftMag_spectralEntropy_sma','pcm_fftMag_spectralVariance_sma','pcm_fftMag_spectralSkewness_sma','pcm_fftMag_spectralKurtosis_sma','pcm_fftMag_spectralSlope_sma','pcm_fftMag_psySharpness_sma','pcm_fftMag_spectralHarmonicity_sma','mfcc_sma[1]','mfcc_sma[2]','mfcc_sma[3]','mfcc_sma[4]','mfcc_sma[5]','mfcc_sma[6]','mfcc_sma[7]','mfcc_sma[8]','mfcc_sma[9]','mfcc_sma[10]','mfcc_sma[11]','mfcc_sma[12]','mfcc_sma[13]','mfcc_sma[14]','isTurn']

def check_file(head):

    # perfect match
    if len(feat_names) == len(head) and set(feat_names) == set(head):
        val = 1
    else:
        # duplicates present, but there is a match
        if set(feat_names).issubset(head):
            val = 2
        # matches with emobase config
        if set(emobase_feat_names) == set(head):
            val = 3
        # different configs, no match or missing features
        else:
            val = 0
    return val


path = sys.argv[1]
meta_path = sys.argv[2]
part_id_ = glob(path + "*/")

extension = "csv.gz"
#extension = "csv"

for item in part_id_:
    df = pd.DataFrame(columns=cols)
    files = glob(item + "*." + extension)
    for i, file_ in enumerate(files):
        # try to just read the first line (header info) of the csv file
        # if this fails, that is because the file is empty
        try:
            header_info = pd.read_csv(file_, nrows=0, sep=";").columns.values
            df.loc[i, "file_path"] = file_
            df.loc[i, "unixtime"] = file_.split("/")[-1].split("_")[-1].rsplit("." + extension)[0]
            df.loc[i, "time_UTC"] = datetime.utcfromtimestamp(int(df.loc[i, "unixtime"])/1000)
            df.loc[i, "file_check"] = check_file(header_info)

        except:
            df.loc[i, "file_path"] = file_
            df.loc[i, "unixtime"] = file_.split("/")[-1].split("_")[-1].rsplit("." + extension)[0]
            df.loc[i, "time_UTC"] = datetime.utcfromtimestamp(int(df.loc[i, "unixtime"])/1000)
            df.loc[i, "file_check"] = -1
    df.to_csv(meta_path + item.split("/")[-2] + ".csv")

